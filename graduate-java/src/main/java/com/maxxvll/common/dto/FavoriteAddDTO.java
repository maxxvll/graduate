package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加收藏请求DTO
 * <p>
 * 用于添加消息收藏时的请求参数验证。
 * 包含消息ID及其相关信息。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "添加收藏请求参数")
public class FavoriteAddDTO {

    /**
     * 消息ID
     */
    @Schema(description = "消息ID", example = "12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容", example = "这是一条重要的消息")
    private String content;

    /**
     * 消息类型：TEXT/IMAGE/FILE/VOICE
     */
    @Schema(description = "消息类型", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "FILE", "VOICE"})
    private String messageType;

    /**
     * 文件URL
     */
    @Schema(description = "文件URL", example = "https://example.com/file.jpg")
    private String fileUrl;

    /**
     * 发送者ID
     */
    @Schema(description = "发送者ID", example = "10001")
    private Long senderId;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID", example = "session_001")
    private Long sessionId;
}
