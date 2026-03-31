package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.event.SessionUpdateEvent;
import com.maxxvll.common.vo.CursorPageVO;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    private static final int SESSION_PREVIEW_MAX_LENGTH = 250;
    private static final int DEFAULT_SESSION_PAGE_LIMIT = 100;
    private static final int MAX_SESSION_PAGE_LIMIT = 200;

    @Resource
    private ChatUserMapper chatUserMapper;

    @Resource
    private ChatGroupMapper chatGroupMapper;

    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionAfterSend(ChatMessage chatMessage) {
        if (chatMessage == null || StrUtil.isBlank(chatMessage.getSessionId())) {
            log.warn("Skip session update because message is empty");
            return;
        }

        String senderId = chatMessage.getSenderId();
        if (StrUtil.isBlank(senderId)) {
            log.warn("Skip session update because senderId is empty, sessionId={}", chatMessage.getSessionId());
            return;
        }

        Integer sessionType = resolveSessionType(chatMessage);
        String lastMessageContent = buildSessionPreview(chatMessage.getContent());
        Date now = new Date();

        if (SessionType.GROUP.getCode().equals(sessionType)) {
            String groupId = resolveGroupId(chatMessage);
            upsertSession(senderId, groupId, chatMessage, sessionType, lastMessageContent, 0, "Group Chat", null, now);
            baseMapper.refreshGroupSessionsAfterSend(chatMessage, senderId, lastMessageContent, now);
            return;
        }

        String receiverId = resolveSingleReceiverId(chatMessage);
        if (StrUtil.isBlank(receiverId)) {
            log.warn("Skip single session update because receiverId is empty, sessionId={}", chatMessage.getSessionId());
            return;
        }

        upsertSession(senderId, receiverId, chatMessage, sessionType, lastMessageContent, 0, "Session", null, now);
        upsertSession(receiverId, senderId, chatMessage, sessionType, lastMessageContent, 1, "Session", null, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnreadCount(String sessionId, String userId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getUnreadCount, 0)
                .set(ChatSession::getUpdateTime, new Date());

        int updateCount = baseMapper.update(null, updateWrapper);
        log.info("Clear unread count success, sessionId={}, userId={}, updatedRows={}", sessionId, userId, updateCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionAfterMessageRevoked(ChatMessage revokedMessage) {
        if (revokedMessage == null || revokedMessage.getId() == null || StrUtil.isBlank(revokedMessage.getSessionId())) {
            log.warn("Skip revoked session refresh because revokedMessage is invalid");
            return;
        }

        int updateCount = baseMapper.refreshRevokedMessagePreview(
                revokedMessage.getSessionId(),
                revokedMessage.getId(),
                "[Message revoked]",
                new Date()
        );
        log.info("Refresh revoked message preview, sessionId={}, updatedRows={}",
                revokedMessage.getSessionId(), updateCount);
    }

    @Override
    public List<SessionVO> getSessionList(String userId) {
        return getSessionPage(userId, DEFAULT_SESSION_PAGE_LIMIT, null).getItems();
    }

    @Override
    public CursorPageVO<SessionVO> getSessionPage(String userId, int limit, String cursor) {
        int normalizedLimit = normalizeSessionLimit(limit);
        SessionPageCursor pageCursor = parsePageCursor(cursor);
        List<ChatSession> sessionList = baseMapper.selectRecentSessions(
                userId,
                normalizedLimit + 1,
                pageCursor != null ? pageCursor.isTop() : null,
                pageCursor != null ? new Date(pageCursor.sortTimeMillis()) : null,
                pageCursor != null ? pageCursor.id() : null
        );

        boolean hasMore = sessionList.size() > normalizedLimit;
        if (hasMore) {
            sessionList = sessionList.subList(0, normalizedLimit);
        }

        return CursorPageVO.<SessionVO>builder()
                .items(buildSessionVOs(userId, sessionList))
                .nextCursor(hasMore && !sessionList.isEmpty() ? buildPageCursor(sessionList.get(sessionList.size() - 1)) : null)
                .hasMore(hasMore)
                .build();
    }

    @Override
    public CursorPageVO<SessionVO> syncSessionList(String userId, int limit, String cursor) {
        int normalizedLimit = normalizeSessionLimit(limit);
        SessionSyncCursor syncCursor = parseSyncCursor(cursor);
        List<ChatSession> sessionList = baseMapper.selectUpdatedSessions(
                userId,
                normalizedLimit + 1,
                syncCursor != null ? new Date(syncCursor.updatedAtMillis()) : null,
                syncCursor != null ? syncCursor.id() : null
        );

        boolean hasMore = sessionList.size() > normalizedLimit;
        if (hasMore) {
            sessionList = sessionList.subList(0, normalizedLimit);
        }

        String nextCursor = sessionList.isEmpty()
                ? (syncCursor != null ? cursor : null)
                : buildSyncCursor(sessionList.get(sessionList.size() - 1));

        return CursorPageVO.<SessionVO>builder()
                .items(buildSessionVOs(userId, sessionList))
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private List<SessionVO> buildSessionVOs(String userId, List<ChatSession> sessionList) {
        if (sessionList == null || sessionList.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> derivedTargetIdMap = new HashMap<>();
        for (ChatSession session : sessionList) {
            String derived = deriveTargetId(userId, session.getSessionId(), session.getSessionType());
            if (StrUtil.isNotBlank(derived)) {
                derivedTargetIdMap.put(session.getSessionId(), derived);
            }
        }

        Set<String> singleTargetIds = new HashSet<>();
        for (ChatSession session : sessionList) {
            if (!isGroupSession(session)) {
                String derived = derivedTargetIdMap.get(session.getSessionId());
                if (StrUtil.isNotBlank(derived)) {
                    singleTargetIds.add(derived);
                }
                if (StrUtil.isNotBlank(session.getTargetId())) {
                    singleTargetIds.add(session.getTargetId());
                }
            }
        }

        Map<String, ChatUser> userMap = new HashMap<>();
        if (!singleTargetIds.isEmpty()) {
            chatUserMapper.selectList(
                    new LambdaQueryWrapper<ChatUser>()
                            .in(ChatUser::getId, singleTargetIds)
                            .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar)
            ).forEach(user -> userMap.put(user.getId(), user));
        }

        Set<String> groupTargetIds = new HashSet<>();
        for (ChatSession session : sessionList) {
            if (isGroupSession(session)) {
                String derived = derivedTargetIdMap.get(session.getSessionId());
                if (StrUtil.isNotBlank(derived)) {
                    groupTargetIds.add(derived);
                }
                if (StrUtil.isNotBlank(session.getTargetId())) {
                    groupTargetIds.add(session.getTargetId());
                }
            }
        }

        Map<Long, ChatGroup> groupMap = new HashMap<>();
        if (!groupTargetIds.isEmpty()) {
            chatGroupMapper.selectList(
                    new LambdaQueryWrapper<ChatGroup>()
                            .in(ChatGroup::getId, groupTargetIds)
                            .select(ChatGroup::getId, ChatGroup::getGroupName, ChatGroup::getGroupAvatar)
            ).forEach(group -> groupMap.put(group.getId(), group));
        }

        Set<String> senderIds = sessionList.stream()
                .map(ChatSession::getLastMessageSenderId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, String> senderNicknameMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            chatUserMapper.selectList(
                    new LambdaQueryWrapper<ChatUser>()
                            .in(ChatUser::getId, senderIds)
                            .select(ChatUser::getId, ChatUser::getNickname)
            ).forEach(user -> senderNicknameMap.put(user.getId(), user.getNickname()));
        }

        // 批量收集所有需要构建 URL 的头像文件名（优化 N+1 查询）
        Map<String, String> allAvatarFiles = new HashMap<>();
        for (ChatSession session : sessionList) {
            String derivedTargetId = derivedTargetIdMap.get(session.getSessionId());

            // 收集单聊用户头像
            if (!isGroupSession(session)) {
                ChatUser targetUser = userMap.get(derivedTargetId);
                if (targetUser == null && StrUtil.isNotBlank(session.getTargetId())) {
                    targetUser = userMap.get(session.getTargetId());
                }
                if (targetUser != null && StrUtil.isNotBlank(targetUser.getAvatar())) {
                    allAvatarFiles.put("user_" + session.getId(), targetUser.getAvatar());
                }
            } else {
                // 收集群头像
                ChatGroup group = groupMap.get(parseLongSafely(derivedTargetId));
                if (group == null && StrUtil.isNotBlank(session.getTargetId())) {
                    group = groupMap.get(parseLongSafely(session.getTargetId()));
                }
                if (group != null && StrUtil.isNotBlank(group.getGroupAvatar())) {
                    allAvatarFiles.put("group_" + session.getId(), group.getGroupAvatar());
                }
            }

            // 收集会话头像（可能从数据库直接获取的原始文件名）
            if (StrUtil.isNotBlank(session.getSessionAvatar()) && !session.getSessionAvatar().startsWith("http")) {
                allAvatarFiles.put("session_" + session.getId(), session.getSessionAvatar());
            }
        }

        // 批量获取头像 URL
        Map<String, String> avatarUrlMap = minioUtil.getAvatarUrlsBatch(allAvatarFiles);

        return sessionList.stream().map(session -> {
            SessionVO vo = BeanConvertUtil.convert(session, SessionVO.class);
            String derivedTargetId = derivedTargetIdMap.get(session.getSessionId());

            if (!isGroupSession(session)) {
                ChatUser targetUser = userMap.get(derivedTargetId);
                if (targetUser == null && StrUtil.isNotBlank(session.getTargetId())) {
                    targetUser = userMap.get(session.getTargetId());
                }
                if (targetUser != null) {
                    vo.setSessionName(targetUser.getNickname());
                    // 使用批量获取的 URL
                    String avatarUrl = avatarUrlMap.get("user_" + session.getId());
                    vo.setSessionAvatar(StrUtil.isNotBlank(avatarUrl) ? avatarUrl : minioUtil.getAvatarUrl(targetUser.getAvatar()));
                }
            } else {
                ChatGroup group = groupMap.get(parseLongSafely(derivedTargetId));
                if (group == null && StrUtil.isNotBlank(session.getTargetId())) {
                    group = groupMap.get(parseLongSafely(session.getTargetId()));
                }
                if (group != null) {
                    vo.setSessionName(group.getGroupName());
                    // 使用批量获取的 URL
                    String avatarUrl = avatarUrlMap.get("group_" + session.getId());
                    vo.setSessionAvatar(StrUtil.isNotBlank(avatarUrl) ? avatarUrl : minioUtil.getAvatarUrl(group.getGroupAvatar()));
                }
            }

            // 如果批量未覆盖到，回退到原逻辑处理
            String currentAvatar = vo.getSessionAvatar();
            if (StrUtil.isBlank(currentAvatar) || !currentAvatar.startsWith("http")) {
                String sessionAvatarUrl = avatarUrlMap.get("session_" + session.getId());
                if (StrUtil.isNotBlank(sessionAvatarUrl)) {
                    vo.setSessionAvatar(sessionAvatarUrl);
                } else if (StrUtil.isNotBlank(currentAvatar) && !currentAvatar.startsWith("http")) {
                    vo.setSessionAvatar(minioUtil.getAvatarUrl(currentAvatar));
                }
            }

            String senderId = session.getLastMessageSenderId();
            if (StrUtil.isNotBlank(senderId)) {
                vo.setLastMessageSenderName(senderId.equals(userId)
                        ? "Me"
                        : senderNicknameMap.getOrDefault(senderId, ""));
            }

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteSession(String sessionId, String userId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .set(ChatSession::getIsDeleted, 1)
                .set(ChatSession::getUpdateTime, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("Soft delete session success, sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTopStatus(String sessionId, String userId, Integer isTop) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getIsTop, isTop)
                .set(ChatSession::getUpdateTime, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("Update top status success, sessionId={}, userId={}, isTop={}", sessionId, userId, isTop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMuteStatus(String sessionId, String userId, Integer isMute) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getIsMute, isMute)
                .set(ChatSession::getUpdateTime, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("Update mute status success, sessionId={}, userId={}, isMute={}", sessionId, userId, isMute);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initFriendSession(String applicantId, String handlerId, String sessionId,
                                  ChatUser applicantInfo, ChatUser handlerInfo) {
        ensureSessionRow(applicantId, handlerId, sessionId, SessionType.SINGLE.getCode(),
                handlerInfo.getNickname(), handlerInfo.getAvatar());
        ensureSessionRow(handlerId, applicantId, sessionId, SessionType.SINGLE.getCode(),
                applicantInfo.getNickname(), applicantInfo.getAvatar());
        log.info("Init friend session success, sessionId={}, applicantId={}, handlerId={}",
                sessionId, applicantId, handlerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initGroupMemberSession(String userId, String groupId, String sessionId,
                                       String groupName, String groupAvatar) {
        ensureSessionRow(userId, groupId, sessionId, SessionType.GROUP.getCode(), groupName, groupAvatar);
        log.info("Init group member session success, sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAllLastMessage(ChatMessage message) {
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatSession::getSessionId, message.getSessionId())
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getLastMessageId, message.getId())
                .set(ChatSession::getLastMessageContent, buildSessionPreview(message.getContent()))
                .set(ChatSession::getLastMessageTime, message.getSendTime())
                .set(ChatSession::getLastMessageSenderId, message.getSenderId())
                .set(ChatSession::getUpdateTime, new Date());
        baseMapper.update(null, wrapper);
        log.info("Refresh last message success, sessionId={}", message.getSessionId());
    }

    private void upsertSession(String userId,
                               String targetId,
                               ChatMessage message,
                               Integer sessionType,
                               String lastMessageContent,
                               int unreadIncrement,
                               String defaultSessionName,
                               String defaultSessionAvatar,
                               Date now) {
        baseMapper.upsertSessionAfterSend(
                userId,
                targetId,
                message.getSessionId(),
                sessionType,
                defaultSessionName,
                defaultSessionAvatar,
                lastMessageContent,
                unreadIncrement,
                message,
                now
        );
    }

    private Integer resolveSessionType(ChatMessage message) {
        if (message.getSessionType() != null) {
            return message.getSessionType();
        }
        return message.getSessionId() != null && message.getSessionId().startsWith("group_")
                ? SessionType.GROUP.getCode()
                : SessionType.SINGLE.getCode();
    }

    private String resolveSingleReceiverId(ChatMessage message) {
        if (StrUtil.isNotBlank(message.getReceiverId())) {
            return message.getReceiverId();
        }
        return deriveTargetId(message.getSenderId(), message.getSessionId(), resolveSessionType(message));
    }

    private String resolveGroupId(ChatMessage message) {
        if (StrUtil.isNotBlank(message.getReceiverId())) {
            return message.getReceiverId();
        }
        return deriveTargetId(message.getSenderId(), message.getSessionId(), resolveSessionType(message));
    }

    private String buildSessionPreview(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= SESSION_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, SESSION_PREVIEW_MAX_LENGTH);
    }

    private String deriveTargetId(String userId, String sessionId, Integer sessionType) {
        if (sessionId == null) {
            return null;
        }

        boolean isGroup = SessionType.GROUP.getCode().equals(sessionType)
                || (sessionType == null && sessionId.startsWith("group_"));
        if (isGroup) {
            return sessionId.startsWith("group_") ? sessionId.substring(6) : null;
        }

        int idx = sessionId.indexOf('_');
        if (idx < 0) {
            return null;
        }

        String part0 = sessionId.substring(0, idx);
        String part1 = sessionId.substring(idx + 1);
        return userId.equals(part0) ? part1 : part0;
    }

    private boolean isGroupSession(ChatSession session) {
        if (SessionType.GROUP.getCode().equals(session.getSessionType())) {
            return true;
        }
        if (SessionType.SINGLE.getCode().equals(session.getSessionType())) {
            return false;
        }
        return session.getSessionId() != null && session.getSessionId().startsWith("group_");
    }

    private Long parseLongSafely(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int normalizeSessionLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SESSION_PAGE_LIMIT;
        }
        return Math.min(limit, MAX_SESSION_PAGE_LIMIT);
    }

    private String buildPageCursor(ChatSession session) {
        return (session.getIsTop() != null ? session.getIsTop() : 0)
                + ":" + coalesceTime(session.getLastMessageTime(), session.getCreateTime())
                + ":" + session.getId();
    }

    private String buildSyncCursor(ChatSession session) {
        return coalesceTime(session.getUpdateTime(), session.getCreateTime()) + ":" + session.getId();
    }

    private SessionPageCursor parsePageCursor(String cursor) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }

        String[] parts = cursor.split(":");
        if (parts.length != 3) {
            log.warn("Ignore invalid session page cursor: {}", cursor);
            return null;
        }

        try {
            return new SessionPageCursor(
                    Integer.parseInt(parts[0]),
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2])
            );
        } catch (NumberFormatException e) {
            log.warn("Ignore invalid session page cursor: {}", cursor);
            return null;
        }
    }

    private SessionSyncCursor parseSyncCursor(String cursor) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }

        String[] parts = cursor.split(":");
        if (parts.length != 2) {
            log.warn("Ignore invalid session sync cursor: {}", cursor);
            return null;
        }

        try {
            return new SessionSyncCursor(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1])
            );
        } catch (NumberFormatException e) {
            log.warn("Ignore invalid session sync cursor: {}", cursor);
            return null;
        }
    }

    private long coalesceTime(Date primary, Date fallback) {
        if (primary != null) {
            return primary.getTime();
        }
        if (fallback != null) {
            return fallback.getTime();
        }
        return 0L;
    }

    private void ensureSessionRow(String userId, String targetId, String sessionId,
                                  int sessionType, String sessionName, String sessionAvatar) {
        boolean exists = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getIsDeleted, 0)
                .exists();
        if (!exists) {
            ChatSession session = new ChatSession();
            session.setSessionId(sessionId);
            session.setSessionType(sessionType);
            session.setUserId(userId);
            session.setTargetId(targetId);
            session.setSessionName(sessionName);
            session.setSessionAvatar(sessionAvatar);
            session.setUnreadCount(0);
            session.setIsTop(0);
            session.setIsMute(0);
            session.setIsHide(0);
            session.setIsDeleted(0);
            session.setCreateTime(new Date());
            session.setUpdateTime(new Date());
            save(session);
        }
    }

    private record SessionPageCursor(Integer isTop, long sortTimeMillis, Long id) {
    }

    private record SessionSyncCursor(long updatedAtMillis, Long id) {
    }

    @EventListener
    @Async
    public void handleSessionUpdateEvent(SessionUpdateEvent event) {
        try {
            switch (event.getType()) {
                case MESSAGE_SENT -> updateSessionAfterSend(event.getMessage());
                case MESSAGE_REVOKED -> updateSessionAfterMessageRevoked(event.getMessage());
                case MESSAGE_EDITED -> updateSessionAfterMessageEdited(event.getMessage());
                case CLEAR_UNREAD -> clearUnreadCount(event.getSessionId(), event.getUserId());
                case REFRESH_LAST_MESSAGE -> refreshAllLastMessage(event.getMessage());
                default -> log.warn("Unknown session update event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Handle session update event failed, type={}, sessionId={}",
                    event.getType(), event.getSessionId(), e);
        }
    }

    /**
     * 处理消息编辑后的会话更新
     */
    private void updateSessionAfterMessageEdited(ChatMessage message) {
        if (message == null) return;
        String sessionId = message.getSessionId();
        if (sessionId == null) return;

        // 更新会话的最后一条消息和更新时间
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .set(ChatSession::getLastMessageContent, message.getContent())
                .set(ChatSession::getLastMessageTime, message.getSendTime());

        baseMapper.update(null, updateWrapper);
        log.debug("会话 {} 的最后消息已更新（消息编辑）", sessionId);
    }

    @Override
    public List<ChatSession> getSessionsByUserId(String userId) {
        return this.list(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getIsDeleted, 0)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }
}
