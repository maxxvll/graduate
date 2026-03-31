package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建群聊请求DTO
 * <p>
 * 用于创建群聊时的请求参数验证。
 * 包含群名称、群头像、最大成员数、加群方式和初始成员列表。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "创建群聊请求参数")
public class GroupCreateDTO {

    /**
     * 群名称
     */
    @Schema(description = "群名称", example = "我的群聊", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "群名称不能为空")
    @Size(max = 50, message = "群名称长度不能超过50个字符")
    private String groupName;

    /**
     * 群头像URL
     */
    @Schema(description = "群头像URL", example = "https://example.com/group-avatar.jpg")
    @Size(max = 500, message = "群头像URL长度不能超过500个字符")
    private String groupAvatar;

    /**
     * 群最大成员数
     */
    @Schema(description = "群最大成员数", example = "200")
    @Size(min = 2, max = 500, message = "群成员数必须在2-500人之间")
    private Integer maxMember;

    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    @Schema(description = "加群方式", example = "1", allowableValues = {"1", "2", "3"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "加群方式不能为空")
    @Size(min = 1, max = 3, message = "加群方式必须为1、2或3")
    private Integer joinType;

    /**
     * 初始成员ID列表
     */
    @Schema(description = "初始成员ID列表", example = "[\"10001\",\"10002\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "群成员列表不能为空")
    private List<String> memberIds;
}
