package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片消息处理器
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class ImageMessageHandler implements MessageTypeHandler {

    @Override
    public boolean supports(MessageType type) {
        return MessageType.IMAGE == type;
    }

    @Override
    public void handleBeforeSend(ChatMessage message) {
        // 图片消息需要确保文件URL已设置
        if (message.getFileUrl() == null || message.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("图片消息必须包含文件URL");
        }
        log.debug("Image message processed: {}", message.getId());
    }

    @Override
    public String getDefaultContent() {
        return "[图片]";
    }

    @Override
    public boolean requiresFileUrl() {
        return true;
    }
}
