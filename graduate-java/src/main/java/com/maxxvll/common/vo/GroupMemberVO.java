package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员信息响应VO
 * <p>
 * 用于返回群成员详细信息。
 * 包含成员基本信息、角色、加入时间等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "群成员信息")
public class GroupMemberVO {

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 群ID
     */
    @Schema(description = "群ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /**
     * 成员ID
     */
    @Schema(description = "成员ID")
    private String userId;

    /**
     * 成员昵称
     */
    @Schema(description = "成员昵称")
    private String nickname;

    /**
     * 成员头像URL
     */
    @Schema(description = "成员头像URL")
    private String avatar;

    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    @Schema(description = "成员角色")
    private Integer role;

    /**
     * 角色名称
     */
    @Schema(description = "角色名称")
    private String roleName;

    /**
     * 加入时间
     */
    @Schema(description = "加入时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinTime;

    /**
     * 邀请人ID
     */
    @Schema(description = "邀请人ID")
    private String inviterId;

    /**
     * 邀请人昵称
     */
    @Schema(description = "邀请人昵称")
    private String inviterNickname;

    /**
     * 是否禁言：0-否，1-是
     */
    @Schema(description = "是否禁言")
    private Integer isMute;
}
