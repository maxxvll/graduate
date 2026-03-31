package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申请加入群聊请求DTO
 * <p>
 * 用于申请加入群聊时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "申请加入群聊请求参数")
public class GroupApplyDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 申请备注信息
     */
    @Schema(description = "申请备注", example = "请通过我的申请")
    @Size(max = 100, message = "申请备注不能超过100个字符")
    private String remark;
}
