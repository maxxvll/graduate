package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友申请表
 * @TableName friend_application
 */
@TableName(value = "friend_application")
@Data
@EqualsAndHashCode(callSuper = true)
public class FriendApplication extends BaseEntity {

    /**
     * 申请人ID（关联用户表user.id）
     */
    private Long applicantId;

    /**
     * 被申请人ID（关联用户表user.id）
     */
    private Long targetUserId;

    /**
     * 申请状态：0-待处理 1-已通过 2-已拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（仅状态为2时填写）
     */
    private String rejectReason;

    /**
     * 申请备注（申请人所写的验证信息）
     */
    private String remark;
}
