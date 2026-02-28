package com.maxxvll.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxxvll.common.vo.SessionVO;
import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;

import java.util.List;

/**
 * @author 20570
 * @description 针对表【chat_session(聊天会话表（支撑聊天列表）)】的数据库操作Service
 * @createDate 2026-02-19 12:02:21
 */
public interface ChatSessionService extends IService<ChatSession> {

    void updateSessionAfterSend(ChatMessage chatMessage);

    /**
     * 清除会话未读数
     */
    void clearUnreadCount(String sessionId, String userId);

    /**
     * 消息撤回后更新会话
     */
    void updateSessionAfterMessageRevoked(ChatMessage revokedMessage);

    /**
     * 获取当前用户的会话列表（已排序：置顶优先，再按最后消息时间倒序）
     */
    List<SessionVO> getSessionList(String userId);

    /**
     * 软删除会话（仅对当前用户隐藏）
     */
    void softDeleteSession(String sessionId, String userId);

    /**
     * 更新会话置顶状态
     */
    void updateTopStatus(String sessionId, String userId, Integer isTop);

    /**
     * 更新会话免打扰状态
     */
    void updateMuteStatus(String sessionId, String userId, Integer isMute);
}
