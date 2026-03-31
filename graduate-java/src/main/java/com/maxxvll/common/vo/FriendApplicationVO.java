package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请信息响应VO
 * <p>
 * 用于返回好友申请相关信息。
 * 包含申请人信息、被申请人信息、申请状态等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "好友申请信息")
public class FriendApplicationVO {

    /**
     * 申请ID
     */
    @Schema(description = "申请ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 申请人ID
     */
    @Schema(description = "申请人ID")
    private String applicantId;

    /**
     * 申请人昵称
     */
    @Schema(description = "申请人昵称")
    private String applicantNickname;

    /**
     * 申请人用户名
     */
    @Schema(description = "申请人用户名")
    private String applicantUsername;

    /**
     * 申请人头像URL
     */
    @Schema(description = "申请人头像URL")
    private String applicantAvatar;

    /**
     * 备注名
     */
    @Schema(description = "备注名")
    private String remarkName;

    /**
     * 标签
     */
    @Schema(description = "标签")
    private String tagName;

    /**
     * 权限范围
     */
    @Schema(description = "权限范围")
    private Integer permissionScope;

    /**
     * 是否星标
     */
    @Schema(description = "是否星标")
    private Boolean starred;

    /**
     * 是否拉黑
     */
    @Schema(description = "是否拉黑")
    private Boolean blacklisted;

    /**
     * 被申请人ID
     */
    @Schema(description = "被申请人ID")
    private String targetUserId;

    /**
     * 被申请人昵称
     */
    @Schema(description = "被申请人昵称")
    private String targetNickname;

    /**
     * 被申请人用户名
     */
    @Schema(description = "被申请人用户名")
    private String targetUsername;

    /**
     * 被申请人头像URL
     */
    @Schema(description = "被申请人头像URL")
    private String targetAvatar;

    /**
     * 申请备注
     */
    @Schema(description = "申请备注")
    private String remark;

    /**
     * 申请状态：0-待处理，1-已通过，2-已拒绝
     */
    @Schema(description = "申请状态")
    private Integer status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusDesc;

    /**
     * 申请时间
     */
    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
