package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊信息响应VO
 * <p>
 * 用于返回群聊详细信息。
 * 包含群基本信息、成员统计、当前用户权限等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "群聊信息")
public class GroupInfoVO {

    /**
     * 群ID
     */
    @Schema(description = "群ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 群名称
     */
    @Schema(description = "群名称")
    private String groupName;

    /**
     * 群头像URL
     */
    @Schema(description = "群头像URL")
    private String groupAvatar;

    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    /**
     * 创建人昵称
     */
    @Schema(description = "创建人昵称")
    private String creatorNickname;

    /**
     * 群最大成员数
     */
    @Schema(description = "群最大成员数")
    private Integer maxMember;

    /**
     * 当前成员数
     */
    @Schema(description = "当前成员数")
    private Integer currentMemberCount;

    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    @Schema(description = "加群方式")
    private Integer joinType;

    /**
     * 群公告
     */
    @Schema(description = "群公告")
    private String notice;

    /**
     * 是否全员禁言：0-否，1-是
     */
    @Schema(description = "是否全员禁言")
    private Integer isMuteAll;

    /**
     * 群状态：1-正常，2-解散，3-封禁
     */
    @Schema(description = "群状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 当前用户在群中的角色：1-群主，2-管理员，3-普通成员，0-非成员
     */
    @Schema(description = "当前用户角色")
    private Integer myRole;

    /**
     * 当前用户申请状态：null-未申请，member-已是成员，pending-申请待审核
     */
    @Schema(description = "申请状态")
    private String applyStatus;
}
