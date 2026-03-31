package com.maxxvll.service.message.template;

import com.maxxvll.common.dto.ChatMessageSendDTO;
import com.maxxvll.service.ChatMessageService;

/**
 * 消息发送上下文
 * 包含消息发送所需的所有信息
 *
 * @author maxxvll
 * @since 2026-03-31
 */
public class MessageSendContext {

    private final ChatMessageSendDTO sendDTO;
    private final String senderId;
    private final ChatMessageService messageService;

    public MessageSendContext(ChatMessageSendDTO sendDTO, String senderId, ChatMessageService messageService) {
        this.sendDTO = sendDTO;
        this.senderId = senderId;
        this.messageService = messageService;
    }

    public ChatMessageSendDTO getSendDTO() {
        return sendDTO;
    }

    public String getSenderId() {
        return senderId;
    }

    public ChatMessageService getMessageService() {
        return messageService;
    }
}
