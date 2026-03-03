package com.maxxvll.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 好友申请信息VO
 */
@Data
public class FriendApplicationVO {

    /** 申请 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 申请人ID */
    private String applicantId;

    /** 申请人昵称 */
    private String applicantNickname;

    /** 申请人头像 */
    private String applicantAvatar;

    /** 被申请人ID */
    private String targetUserId;

    /** 被申请人昵称 */
    private String targetNickname;

    /** 被申请人头像 */
    private String targetAvatar;

    /** 申请备注 */
    private String remark;

    /** 申请状态：0-待处理 1-已通过 2-已拒绝 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
