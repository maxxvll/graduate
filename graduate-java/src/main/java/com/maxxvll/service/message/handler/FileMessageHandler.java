package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文件消息处理器
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class FileMessageHandler implements MessageTypeHandler {

    @Override
    public boolean supports(MessageType type) {
        return MessageType.FILE == type;
    }

    @Override
    public void handleBeforeSend(ChatMessage message) {
        if (message.getFileUrl() == null || message.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("文件消息必须包含文件URL");
        }
        if (message.getFileName() == null || message.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("文件消息必须包含文件名");
        }
        log.debug("File message processed: {}, fileName: {}", message.getId(), message.getFileName());
    }

    @Override
    public String getDefaultContent() {
        return "[文件]";
    }

    @Override
    public boolean requiresFileUrl() {
        return true;
    }
}
