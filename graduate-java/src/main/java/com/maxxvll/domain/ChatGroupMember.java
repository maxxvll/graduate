package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 群成员关联表（核心多对多）
 * @TableName chat_group_member
 */
@TableName("chat_group_member")
@Data
@EqualsAndHashCode(callSuper = false)
public class ChatGroupMember extends BaseEntity {

    /**
     * 群ID（关联chat_group.id）
     */
    private Long groupId;

    /**
     * 成员ID（关联chat_user.id）
     */
    private String userId;

    /**
     * 成员角色：1-群主，2-管理员，3-普通成员
     */
    private Integer role;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 邀请人ID（非邀请加入则为空）
     */
    private String inviterId;

    /**
     * 是否被禁言：0-否，1-是（仅群主/管理员可设置）
     */
    private Integer isMute;

    /**
     * 是否退出：0-未退出，1-已退出
     */
    private Integer isQuit;

    /**
     * 退出时间（未退出则为空）
     */
    private Date quitTime;

    /**
     * 移除原因（管理员/群主移除成员时填写）
     */
    private String quitReason;
}
