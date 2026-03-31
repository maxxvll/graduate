package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群聊基础信息表
 * @TableName chat_group
 */
@TableName("chat_group")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatGroup extends BaseEntity {

    /**
     * 群名称（聊天列表展示）
     */
    private String groupName;

    /**
     * 群头像URL
     */
    private String groupAvatar;

    /**
     * 创建人ID（关联chat_user.id）
     */
    private Long creatorId;

    /**
     * 群最大成员数（默认200，可调整）
     */
    private Integer maxMember;

    /**
     * 加群方式：1-需审核，2-免审核，3-仅邀请
     */
    private Integer joinType;

    /**
     * 群公告
     */
    private String notice;

    /**
     * 是否全员禁言：0-否，1-是
     */
    private Integer isMuteAll;

    /**
     * 群状态：1-正常，2-解散，3-封禁
     */
    private Integer status;

    /**
     * 扩展字段（如群标签、创建原因等）
     */
    private Object extInfo;
}
