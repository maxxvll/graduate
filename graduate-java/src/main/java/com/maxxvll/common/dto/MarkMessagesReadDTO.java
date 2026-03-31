package com.maxxvll.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标记消息已读DTO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class MarkMessagesReadDTO {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 最后阅读的消息ID
     */
    private Long lastReadMessageId;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备唯一标识
     */
    private String deviceId;
}
