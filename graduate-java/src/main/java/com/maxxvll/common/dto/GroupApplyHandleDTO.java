package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理群申请请求DTO
 * <p>
 * 用于处理加群申请（通过/拒绝）时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "处理群申请请求参数")
public class GroupApplyHandleDTO {

    /**
     * 申请ID
     */
    @Schema(description = "申请ID", example = "2001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    /**
     * 处理结果：1-通过，2-拒绝
     */
    @Schema(description = "处理结果", example = "1", allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处理结果不能为空")
    private Integer status;

    /**
     * 拒绝原因（拒绝时填写）
     */
    @Schema(description = "拒绝原因", example = "群已满")
    @Size(max = 100, message = "拒绝原因不能超过100个字符")
    private String rejectReason;
}
