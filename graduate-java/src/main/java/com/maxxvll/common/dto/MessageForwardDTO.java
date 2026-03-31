package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 消息转发请求DTO
 * <p>
 * 用于转发消息到其他会话时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "消息转发请求参数")
public class MessageForwardDTO {

    /**
     * 要转发的消息ID列表
     */
    @Schema(description = "要转发的消息ID列表", example = "[12345, 12346]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "消息ID列表不能为空")
    private List<Long> messageIds;

    /**
     * 目标会话ID
     */
    @Schema(description = "目标会话ID", example = "session_002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标会话ID不能为空")
    private String targetSessionId;
}
