package com.maxxvll.common.dto;

import com.maxxvll.common.annotation.NotRequired;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理好友申请请求DTO
 * <p>
 * 用于处理好友申请（接受/拒绝）时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "处理好友申请请求参数")
public class FriendApplyHandleDTO {

    /**
     * 申请ID
     */
    @Schema(description = "申请ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    /**
     * 处理结果：1-接受，2-拒绝
     */
    @Schema(description = "处理结果", example = "1", allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "处理结果不能为空")
    private Integer status;

    /**
     * 拒绝原因（拒绝时选填）
     */
    @Schema(description = "拒绝原因", example = "暂时不添加好友")
    @Size(max = 100, message = "拒绝原因不能超过100个字符")
    @NotRequired
    private String rejectReason;
}
