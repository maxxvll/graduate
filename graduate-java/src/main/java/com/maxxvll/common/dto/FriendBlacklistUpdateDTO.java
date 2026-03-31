package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 好友黑名单更新请求DTO
 * <p>
 * 用于更新好友黑名单状态时的请求参数验证。
 * 包含好友用户ID和是否拉黑状态。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "好友黑名单更新请求参数")
public class FriendBlacklistUpdateDTO {

    /**
     * 好友用户ID
     */
    @Schema(description = "好友用户ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "好友ID不能为空")
    private String friendUserId;

    /**
     * 是否拉黑
     */
    @Schema(description = "是否拉黑", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "黑名单状态不能为空")
    private Boolean blacklisted;
}
