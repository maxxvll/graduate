package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友申请请求DTO
 * <p>
 * 用于发送好友申请时的请求参数验证。
 * 包含目标用户ID、申请备注和设备类型。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "发送好友申请请求参数")
public class FriendApplyDTO {

    /**
     * 目标用户ID
     */
    @Schema(description = "目标用户ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标用户ID不能为空")
    private String targetId;

    /**
     * 申请备注/申请原因
     */
    @Schema(description = "申请备注", example = "我是XX，想加你为好友")
    @Size(max = 100, message = "申请备注不能超过100个字符")
    private String remark;

    /**
     * 设备类型（前端设备标识，用于区分不同设备的请求）
     */
    @Schema(description = "设备类型", example = "web")
    private String deviceType;
}
