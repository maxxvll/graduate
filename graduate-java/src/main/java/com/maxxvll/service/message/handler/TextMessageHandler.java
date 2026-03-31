package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本消息处理器
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class TextMessageHandler implements MessageTypeHandler {

    @Override
    public boolean supports(MessageType type) {
        return MessageType.TEXT == type;
    }

    @Override
    public void handleBeforeSend(ChatMessage message) {
        // 文本消息无需特殊处理
        log.debug("Text message processed: {}", message.getId());
    }

    @Override
    public String getDefaultContent() {
        return "";
    }
}
