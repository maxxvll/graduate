package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新群成员信息请求DTO
 * <p>
 * 用于更新群成员信息（角色、禁言状态）时的请求参数验证。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "更新群成员信息请求参数")
public class GroupMemberUpdateDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 成员ID
     */
    @Schema(description = "成员ID", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "成员ID不能为空")
    private String userId;

    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    @Schema(description = "成员角色", example = "2", allowableValues = {"1", "2", "3"})
    @Size(min = 1, max = 3, message = "成员角色必须为1、2或3")
    private Integer role;

    /**
     * 是否禁言：0-否，1-是
     */
    @Schema(description = "是否禁言", example = "0", allowableValues = {"0", "1"})
    private Integer isMute;
}