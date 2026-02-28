package com.maxxvll.service;

import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.domain.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
* @author 20570
* @description 针对表【chat_message(聊天消息记录表（精简版）)】的数据库操作Service
* @createDate 2026-02-19 12:02:21
*/
public interface ChatMessageService extends IService<ChatMessage> {
    ChatMessage sendMessage(ChatMessageSendDTO sendDTO, MultipartFile[] files) throws Exception;
    public List<ChatMessage> getMessages(String sessionId);
    // 新增：获取用户的离线消息
    List<ChatMessage> getOfflineMessages(String userId);

    // 新增：标记离线消息为已拉取
    void markOfflineMessagesAsPulled(String userId);

    // 新增：标记会话消息为已读
    void markMessagesAsRead(String sessionId, String userId);

    // 新增：撤回消息
    void revokeMessage(String messageId);

    // 新增：删除消息（软删除）
    void deleteMessage(String messageId);

    void markMessageAsOffline(Long id);
}
