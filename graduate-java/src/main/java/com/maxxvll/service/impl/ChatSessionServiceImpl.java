package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatGroup;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatGroupMapper;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.MinioUtil;
import com.maxxvll.utils.UserContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 20570
 * @description 针对表【chat_session(聊天会话表（支撑聊天列表）)】的数据库操作Service实现
 * @createDate 2026-02-19 12:02:21
 */
@Slf4j
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    @Resource
    private ChatUserMapper chatUserMapper;
    @Resource
    private ChatGroupMapper chatGroupMapper;
    @Resource
    private MinioUtil minioUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionAfterSend(ChatMessage chatMessage) {
        UserInfoVO currentUser = UserContextUtil.getCurrentUser();
        String userId = currentUser.getId();
        updateSession(userId, chatMessage);
    }

    /**
     * 1. 清除会话未读数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnreadCount(String sessionId, String userId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getUnreadCount, 0)
                .set(ChatSession::getUpdatedAt, new Date());

        int updateCount = baseMapper.update(null, updateWrapper);
        log.info("清除会话未读数成功，sessionId: {}, userId: {}, 影响行数: {}", sessionId, userId, updateCount);
    }

    /**
     * 2. 消息撤回后更新会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionAfterMessageRevoked(ChatMessage revokedMessage) {
        String sessionId = revokedMessage.getSessionId();

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getIsDeleted, 0);

        list(wrapper).forEach(session -> {
            if (session.getLastMessageId() != null
                    && session.getLastMessageId().equals(revokedMessage.getId())) {
                session.setLastMessageContent("[消息已撤回]");
                session.setUpdatedAt(new Date());
                updateById(session);
                log.info("更新会话最后一条消息（撤回），sessionId: {}, userId: {}", sessionId, session.getUserId());
            }
        });
    }

    /**
     * 3. 获取当前用户的会话列表
     * 关键优化：
     * - 从 sessionId 字符串解析 targetId（不依赖 DB targetId 字段，历史数据可能错误）
     * - 当 sessionType 为 null 时，通过 sessionId 前缀 "group_" 推断类型（兜底历史脏数据）
     * - 同时将 session.getTargetId() 纳入批量查询，防止前端 BigInt 精度问题导致的 sessionId 偏差
     * - 批量查询用户表和群表，确保头像/名称始终为最新实时数据（避免 N+1）
     * - 兜底：sessionAvatar 若仍为原始短路径，统一转换为完整 URL
     */
    @Override
    public List<SessionVO> getSessionList(String userId) {
        // 查询当前用户未删除的会话，按置顶倒序 + 最后消息时间倒序
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .orderByDesc(ChatSession::getIsTop)
                .orderByDesc(ChatSession::getLastMessageTime);
        List<ChatSession> sessionList = list(wrapper);
        if (sessionList.isEmpty()) {
            return Collections.emptyList();
        }

        // -------- 从 sessionId 字符串推导 targetId（不依赖 DB 字段，历史数据可能错误）--------
        Map<String, String> derivedTargetIdMap = new HashMap<>();
        for (ChatSession s : sessionList) {
            String derived = deriveTargetId(userId, s.getSessionId(), s.getSessionType());
            if (StrUtil.isNotBlank(derived)) {
                derivedTargetIdMap.put(s.getSessionId(), derived);
            }
        }

        // -------- 批量查询：单聊对方用户（derivedTargetId + session.targetId 双保险）--------
        // 同时纳入 session.getTargetId() 防止 JS BigInt 精度丢失导致的推导 ID 与 DB 中存的 targetId 不一致
        Set<String> singleTargetIds = new HashSet<>();
        for (ChatSession s : sessionList) {
            if (!isGroupSession(s)) {
                String derived = derivedTargetIdMap.get(s.getSessionId());
                if (StrUtil.isNotBlank(derived)) singleTargetIds.add(derived);
                // DB 存的 targetId 作为兜底（可能比 derived 更准确）
                if (StrUtil.isNotBlank(s.getTargetId())) singleTargetIds.add(s.getTargetId());
            }
        }

        Map<String, ChatUser> userMap = new HashMap<>();
        if (!singleTargetIds.isEmpty()) {
            chatUserMapper.selectList(
                    new LambdaQueryWrapper<ChatUser>()
                            .in(ChatUser::getId, singleTargetIds)
                            .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar)
            ).forEach(u -> userMap.put(u.getId(), u));
        }

        // -------- 批量查询：群聊（同上，双保险）--------
        Set<String> groupTargetIds = new HashSet<>();
        for (ChatSession s : sessionList) {
            if (isGroupSession(s)) {
                String derived = derivedTargetIdMap.get(s.getSessionId());
                if (StrUtil.isNotBlank(derived)) groupTargetIds.add(derived);
                if (StrUtil.isNotBlank(s.getTargetId())) groupTargetIds.add(s.getTargetId());
            }
        }

        Map<String, ChatGroup> groupMap = new HashMap<>();
        if (!groupTargetIds.isEmpty()) {
            chatGroupMapper.selectList(
                    new LambdaQueryWrapper<ChatGroup>()
                            .in(ChatGroup::getId, groupTargetIds)
                            .select(ChatGroup::getId, ChatGroup::getGroupName, ChatGroup::getGroupAvatar)
            ).forEach(g -> groupMap.put(g.getId(), g));
        }

        // -------- 批量查询：最后消息发送人昵称 --------
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
            ).forEach(u -> senderNicknameMap.put(u.getId(), u.getNickname()));
        }

        return sessionList.stream().map(session -> {
            SessionVO vo = BeanConvertUtil.convert(session, SessionVO.class);
            String derivedTargetId = derivedTargetIdMap.get(session.getSessionId());

            // 动态覆盖 sessionName 和 sessionAvatar：先用推导 ID 查，再用 DB targetId 兜底
            if (!isGroupSession(session)) {
                // 单聊
                ChatUser targetUser = userMap.get(derivedTargetId);
                if (targetUser == null && StrUtil.isNotBlank(session.getTargetId())) {
                    targetUser = userMap.get(session.getTargetId());
                }
                if (targetUser != null) {
                    vo.setSessionName(targetUser.getNickname());
                    vo.setSessionAvatar(minioUtil.getAvatarUrl(targetUser.getAvatar()));
                }
            } else {
                // 群聊
                ChatGroup group = groupMap.get(derivedTargetId);
                if (group == null && StrUtil.isNotBlank(session.getTargetId())) {
                    group = groupMap.get(session.getTargetId());
                }
                if (group != null) {
                    vo.setSessionName(group.getGroupName());
                    vo.setSessionAvatar(minioUtil.getAvatarUrl(group.getGroupAvatar()));
                }
            }

            // 安全兜底：如果 sessionAvatar 仍是原始短路径（未被上面覆盖），强制转换为完整 URL
            // 避免前端收到无法加载的短路径导致头像显示为灰色方块
            String currentAvatar = vo.getSessionAvatar();
            if (StrUtil.isNotBlank(currentAvatar) && !currentAvatar.startsWith("http")) {
                vo.setSessionAvatar(minioUtil.getAvatarUrl(currentAvatar));
            }

            // 设置最后消息发送人名称
            String senderId = session.getLastMessageSenderId();
            if (StrUtil.isNotBlank(senderId)) {
                if (senderId.equals(userId)) {
                    vo.setLastMessageSenderName("我");
                } else {
                    vo.setLastMessageSenderName(
                            senderNicknameMap.getOrDefault(senderId, ""));
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 4. 软删除会话
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteSession(String sessionId, String userId) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .set(ChatSession::getIsDeleted, 1)
                .set(ChatSession::getUpdatedAt, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("软删除会话成功，sessionId: {}, userId: {}", sessionId, userId);
    }

    /**
     * 5. 更新会话置顶状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTopStatus(String sessionId, String userId, Integer isTop) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getIsTop, isTop)
                .set(ChatSession::getUpdatedAt, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("更新会话置顶状态成功，sessionId: {}, userId: {}, isTop: {}", sessionId, userId, isTop);
    }

    /**
     * 6. 更新会话免打扰状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMuteStatus(String sessionId, String userId, Integer isMute) {
        LambdaUpdateWrapper<ChatSession> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getIsMute, isMute)
                .set(ChatSession::getUpdatedAt, new Date());
        baseMapper.update(null, updateWrapper);
        log.info("更新会话免打扰状态成功，sessionId: {}, userId: {}, isMute: {}", sessionId, userId, isMute);
    }

    private void updateSession(String targetUserId, ChatMessage chatMessage) {
        // 修复：用 userId 查询（非 targetId），确保找到正确的会话行
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, targetUserId)
                .eq(ChatSession::getSessionId, chatMessage.getSessionId());
        ChatSession session = getOne(wrapper);
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(chatMessage.getSessionId());
            session.setSessionType(chatMessage.getSessionType());
            session.setUserId(targetUserId);
            // 修复：从 sessionId 派生 targetId，而非错误地使用 senderId
            session.setTargetId(deriveTargetId(targetUserId, chatMessage.getSessionId(), chatMessage.getSessionType()));
            session.setSessionName("会话");
            session.setUnreadCount(0);
            session.setIsTop(0);
            session.setIsMute(0);
            session.setIsHide(0);
            session.setIsDeleted(0);
            session.setCreatedAt(new Date());
        }
        // \u66f4\u65b0\u6700\u540e\u6d88\u606f
        session.setLastMessageId(chatMessage.getId());
        session.setLastMessageContent(chatMessage.getContent());
        session.setLastMessageTime(chatMessage.getSendTime());
        session.setLastMessageSenderId(chatMessage.getSenderId());

        // \u5982\u679c\u4e0d\u662f\u53d1\u9001\u8005\u81ea\u5df1\uff0c\u589e\u52a0\u672a\u8bfb\u6570
        if (!targetUserId.equals(chatMessage.getSenderId())) {
            session.setUnreadCount(session.getUnreadCount() + 1);
        }

        session.setUpdatedAt(new Date());

        saveOrUpdate(session);
    }

    // ==================== \u7cfb\u7edf\u7ea7\u4f1a\u8bdd\u64cd\u4f5c\uff08\u4e0d\u4f9d\u8d56 UserContext\uff09 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initFriendSession(String applicantId, String handlerId, String sessionId,
                                  ChatUser applicantInfo, ChatUser handlerInfo) {
        // \u7533\u8bf7\u4eba\u7684\u4f1a\u8bdd\uff1a\u5bf9\u65b9\u662f handlerInfo
        ensureSessionRow(applicantId, handlerId, sessionId, SessionType.SINGLE.getCode(),
                handlerInfo.getNickname(), handlerInfo.getAvatar());
        // \u88ab\u7533\u8bf7\u4eba\u7684\u4f1a\u8bdd\uff1a\u5bf9\u65b9\u662f applicantInfo
        ensureSessionRow(handlerId, applicantId, sessionId, SessionType.SINGLE.getCode(),
                applicantInfo.getNickname(), applicantInfo.getAvatar());
        log.info("\u521d\u59cb\u5316\u597d\u53cb\u5355\u804a\u4f1a\u8bdd\u6210\u529f\uff0csessionId={}, applicantId={}, handlerId={}",
                sessionId, applicantId, handlerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initGroupMemberSession(String userId, String groupId, String sessionId,
                                       String groupName, String groupAvatar) {
        ensureSessionRow(userId, groupId, sessionId, SessionType.GROUP.getCode(), groupName, groupAvatar);
        log.info("\u521d\u59cb\u5316\u7fa4\u6210\u5458\u4f1a\u8bdd\u6210\u529f\uff0csessionId={}, userId={}", sessionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAllLastMessage(ChatMessage message) {
        LambdaUpdateWrapper<ChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatSession::getSessionId, message.getSessionId())
                .eq(ChatSession::getIsDeleted, 0)
                .set(ChatSession::getLastMessageId, message.getId())
                .set(ChatSession::getLastMessageContent, message.getContent())
                .set(ChatSession::getLastMessageTime, message.getSendTime())
                .set(ChatSession::getLastMessageSenderId, message.getSenderId())
                .set(ChatSession::getUpdatedAt, new Date());
        baseMapper.update(null, wrapper);
        log.info("\u6279\u91cf\u66f4\u65b0\u4f1a\u8bdd\u6700\u540e\u6d88\u606f\u6210\u529f\uff0csessionId={}", message.getSessionId());
    }

    /**
     * \u5982\u679c\u6307\u5b9a\u7528\u6237\u7684\u4f1a\u8bdd\u4e0d\u5b58\u5728\u5219\u521b\u5efa\uff0c\u5df2\u5b58\u5728\u4e0d\u64cd\u4f5c
     */
    /**
     * 从 sessionId 字符串推导对方 ID（不依赖 DB targetId 字段）。
     * <ul>
     *   <li>单聊: sessionId = "minId_maxId"，取不等于 userId 的那段</li>
     *   <li>群聊: sessionId = "group_{groupId}"，取 group_ 后的部分</li>
     * </ul>
     * 当 sessionType 为 null 时，通过 sessionId 前缀推断类型。
     */
    private String deriveTargetId(String userId, String sessionId, Integer sessionType) {
        if (sessionId == null) return null;
        // sessionType 为 GROUP，或 sessionType 为 null 但 sessionId 以 group_ 开头
        boolean isGroup = SessionType.GROUP.getCode().equals(sessionType)
                || (sessionType == null && sessionId.startsWith("group_"));
        if (isGroup) {
            return sessionId.startsWith("group_") ? sessionId.substring(6) : null;
        }
        // 单聊: "A_B"，取不是 userId 的那个
        int idx = sessionId.indexOf('_');
        if (idx < 0) return null;
        String part0 = sessionId.substring(0, idx);
        String part1 = sessionId.substring(idx + 1);
        return userId.equals(part0) ? part1 : part0;
    }

    /**
     * 判断会话是否为群聊。
     * 优先读取 sessionType 字段；如果为 null（历史脏数据），通过 sessionId 前缀推断。
     */
    private boolean isGroupSession(ChatSession session) {
        if (SessionType.GROUP.getCode().equals(session.getSessionType())) return true;
        if (SessionType.SINGLE.getCode().equals(session.getSessionType())) return false;
        // sessionType 为 null：通过 sessionId 前缀推断
        return session.getSessionId() != null && session.getSessionId().startsWith("group_");
    }

    /**
     * 如果指定用户的会话不存在则创建，已存在不操作
     */
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
            session.setCreatedAt(new Date());
            session.setUpdatedAt(new Date());
            save(session);
        }
    }
}