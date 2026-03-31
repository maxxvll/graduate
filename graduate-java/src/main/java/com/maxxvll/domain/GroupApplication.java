package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群申请表
 * @TableName group_application
 */
@TableName(value = "group_application")
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupApplication extends BaseEntity {

    /**
     * 申请人ID（关联用户表user.id）
     */
    private Long applicantId;

    /**
     * 目标群聊ID（关联群表group.id）
     */
    private Long groupId;

    /**
     * 申请状态：0-待处理 1-已通过 2-已拒绝
     */
    private Integer status;

    /**
     * 拒绝原因（仅状态为2时填写）
     */
    private String rejectReason;

    /**
     * 操作人ID（处理申请的群主/管理员，关联用户表user.id，状态为0时为NULL）
     */
    private Long operatorId;
}
