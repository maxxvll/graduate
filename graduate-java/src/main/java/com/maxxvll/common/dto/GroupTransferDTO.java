package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转让群主请求DTO
 * <p>
 * 用于转让群主时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "转让群主请求参数")
public class GroupTransferDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 新群主ID
     */
    @Schema(description = "新群主ID", example = "10002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新群主ID不能为空")
    private String newOwnerId;
}
