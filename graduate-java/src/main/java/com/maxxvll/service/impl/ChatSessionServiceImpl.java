package com.maxxvll.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxxvll.common.enums.SessionType;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.maxxvll.service.ChatSessionService;
import com.maxxvll.mapper.ChatSessionMapper;
import com.maxxvll.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
* @author 20570
* @description 针对表【chat_session(聊天会话表（支撑聊天列表）)】的数据库操作Service实现
* @createDate 2026-02-19 12:02:21
*/
@Slf4j
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

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
     * 逻辑：
     * 1. 判断撤回的消息是否是该会话的最后一条消息
     * 2. 如果是，则更新会话的最后一条消息为上一条消息
     * 3. 如果不是，则不更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSessionAfterMessageRevoked(ChatMessage revokedMessage) {
        String sessionId = revokedMessage.getSessionId();

        // 查询该会话的所有用户（单聊有2条，群聊有多条）
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getIsDeleted, 0);

        list(wrapper).forEach(session -> {
            // 判断撤回的消息是否是该会话的最后一条消息
            if (session.getLastMessageId() != null
                    && session.getLastMessageId().equals(revokedMessage.getId())) {
                // 如果是，则更新会话的最后一条消息为上一条消息
                // 这里简化处理，实际需要查询该会话的倒数第二条消息
                // 暂时先更新为"[消息已撤回]"
                session.setLastMessageContent("[消息已撤回]");
                session.setUpdatedAt(new Date());
                updateById(session);
                log.info("更新会话最后一条消息（撤回），sessionId: {}, userId: {}", sessionId, session.getUserId());
            }
        });
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