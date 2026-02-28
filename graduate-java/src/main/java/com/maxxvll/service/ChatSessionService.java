package com.maxxvll.service;

import com.maxxvll.domain.ChatMessage;
import com.maxxvll.domain.ChatSession;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
