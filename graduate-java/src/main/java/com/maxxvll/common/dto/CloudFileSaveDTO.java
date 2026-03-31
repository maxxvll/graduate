package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 云盘文件保存请求DTO
 * <p>
 * 用于将聊天消息中的文件保存到云盘时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "云盘文件保存请求参数")
public class CloudFileSaveDTO {

    /**
     * 消息ID
     */
    @Schema(description = "消息ID", example = "12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息ID不能为空")
    private Long messageId;

    /**
     * 目标类型
     */
    @Schema(description = "目标类型", example = "personal")
    private String targetType;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID", example = "session_001")
    private String sessionId;
}
