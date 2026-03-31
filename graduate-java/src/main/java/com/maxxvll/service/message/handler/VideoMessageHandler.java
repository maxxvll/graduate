package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 视频消息处理器
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class VideoMessageHandler implements MessageTypeHandler {

    @Override
    public boolean supports(MessageType type) {
        return MessageType.VIDEO == type;
    }

    @Override
    public void handleBeforeSend(ChatMessage message) {
        if (message.getFileUrl() == null || message.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("视频消息必须包含文件URL");
        }
        log.debug("Video message processed: {}", message.getId());
    }

    @Override
    public String getDefaultContent() {
        return "[视频]";
    }

    @Override
    public boolean requiresFileUrl() {
        return true;
    }
}
