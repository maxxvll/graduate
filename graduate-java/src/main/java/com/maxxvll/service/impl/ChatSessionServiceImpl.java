package com.maxxvll.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.domain.ChatUser;
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

        // 收集所有不重复的最后消息发送人 ID，批量查询昵称（避免 N+1 问题）
        Set<String> senderIds = sessionList.stream()
                .map(ChatSession::getLastMessageSenderId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        Map<String, String> senderNicknameMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            LambdaQueryWrapper<ChatUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.in(ChatUser::getId, senderIds)
                    .select(ChatUser::getId, ChatUser::getNickname, ChatUser::getAvatar);
            chatUserMapper.selectList(userWrapper).forEach(u ->
                    senderNicknameMap.put(u.getId(), u.getNickname()));
        }

        return sessionList.stream().map(session -> {
            SessionVO vo = BeanConvertUtil.convert(session, SessionVO.class);

            // 处理头像 URL：如果是短路径，转换为完整 URL
            if (StrUtil.isNotBlank(session.getSessionAvatar())
                    && !session.getSessionAvatar().startsWith("http")) {
                vo.setSessionAvatar(minioUtil.getAvatarUrl(session.getSessionAvatar()));
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
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getTargetId, targetUserId)
                .eq(ChatSession::getSessionId, chatMessage.getSessionId());
        ChatSession session = getOne(wrapper);
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(chatMessage.getSessionId());
            session.setSessionType(chatMessage.getSessionType());
            session.setUserId(targetUserId);
            session.setTargetId(chatMessage.getSessionType().equals(SessionType.SINGLE.getCode())
                    ? chatMessage.getSenderId() : chatMessage.getReceiverId());
            session.setSessionName("会话"); // 实际需要查用户/群表
            session.setUnreadCount(0);
            session.setIsTop(0);
            session.setIsMute(0);
            session.setIsHide(0);
            session.setIsDeleted(0);
        }
        // 更新最后消息
        session.setLastMessageId(chatMessage.getId());
        session.setLastMessageContent(chatMessage.getContent());
        session.setLastMessageTime(chatMessage.getSendTime());
        session.setLastMessageSenderId(chatMessage.getSenderId());

        // 如果不是发送者自己，增加未读数
        if (!targetUserId.equals(chatMessage.getSenderId())) {
            session.setUnreadCount(session.getUnreadCount() + 1);
        }

        session.setUpdatedAt(new Date());

        saveOrUpdate(session);
    }
}