package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 加群申请信息响应VO
 * <p>
 * 用于返回加群申请相关信息。
 * 包含群信息、申请人信息、申请状态等。
 * </p>
 *
 * @author backend-friend
 */
@Data
@Schema(description = "加群申请信息")
public class GroupApplicationVO {

    /**
     * 申请ID
     */
    @Schema(description = "申请ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 群ID
     */
    @Schema(description = "群ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /**
     * 群名称
     */
    @Schema(description = "群名称")
    private String groupName;

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
     * 申请人头像URL
     */
    @Schema(description = "申请人头像URL")
    private String applicantAvatar;

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
     * 拒绝原因
     */
    @Schema(description = "拒绝原因")
    private String rejectReason;

    /**
     * 操作人ID
     */
    @Schema(description = "操作人ID")
    private String operatorId;

    /**
     * 操作人昵称
     */
    @Schema(description = "操作人昵称")
    private String operatorNickname;

    /**
     * 申请备注
     */
    @Schema(description = "申请备注")
    private String remark;

    /**
     * 申请时间
     */
    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 处理时间
     */
    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
