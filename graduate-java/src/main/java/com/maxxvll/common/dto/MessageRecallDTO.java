package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 消息撤回请求DTO
 * <p>
 * 用于撤回消息时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "消息撤回请求参数")
public class MessageRecallDTO {

    /**
     * 要撤回的消息ID
     */
    @Schema(description = "要撤回的消息ID", example = "12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息ID不能为空")
    private Long messageId;
}
