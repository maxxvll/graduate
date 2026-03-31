package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 移除群成员请求DTO
 * <p>
 * 用于移除群成员时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "移除群成员请求参数")
public class GroupMemberRemoveDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 要移除的成员ID
     */
    @Schema(description = "要移除的成员ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "成员ID不能为空")
    private String userId;

    /**
     * 移除原因
     */
    @Schema(description = "移除原因", example = "违反群规")
    @Size(max = 100, message = "移除原因不能超过100个字符")
    private String reason;
}