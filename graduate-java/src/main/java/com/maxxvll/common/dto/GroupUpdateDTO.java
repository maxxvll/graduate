package com.maxxvll.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新群聊信息请求DTO
 * <p>
 * 用于更新群聊信息时的请求参数验证。
 * 所有字段都是可选的，只更新传入的字段。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "更新群聊信息请求参数")
public class GroupUpdateDTO {

    /**
     * 群ID
     */
    @Schema(description = "群ID", example = "3001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "群ID不能为空")
    private Long groupId;

    /**
     * 群名称
     */
    @Schema(description = "群名称", example = "新群名")
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
    private Integer maxMember;

    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    @Schema(description = "加群方式", example = "1", allowableValues = {"1", "2", "3"})
    private Integer joinType;

    /**
     * 群公告
     */
    @Schema(description = "群公告", example = "欢迎加入群聊")
    @Size(max = 500, message = "群公告不能超过500个字符")
    private String notice;

    /**
     * 是否全员禁言：0-否，1-是
     */
    @Schema(description = "是否全员禁言", example = "0", allowableValues = {"0", "1"})
    private Integer isMuteAll;
}
