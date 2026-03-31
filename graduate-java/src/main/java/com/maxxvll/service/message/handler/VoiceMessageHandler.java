package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 音频消息处理器
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class VoiceMessageHandler implements MessageTypeHandler {

    @Override
    public boolean supports(MessageType type) {
        return MessageType.AUDIO == type;
    }

    @Override
    public void handleBeforeSend(ChatMessage message) {
        if (message.getFileUrl() == null || message.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("语音消息必须包含文件URL");
        }
        if (message.getDuration() == null || message.getDuration() <= 0) {
            throw new IllegalArgumentException("语音消息必须包含时长");
        }
        log.debug("Voice message processed: {}, duration: {}s", message.getId(), message.getDuration());
    }

    @Override
    public String getDefaultContent() {
        return "[语音]";
    }

    @Override
    public boolean requiresFileUrl() {
        return true;
    }
}
