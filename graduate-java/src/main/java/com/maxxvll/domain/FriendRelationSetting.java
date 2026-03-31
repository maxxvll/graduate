package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友关系设置表
 * @TableName friend_relation_setting
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friend_relation_setting")
public class FriendRelationSetting extends BaseEntity {

    /**
     * 拥有者用户ID
     */
    private Long ownerUserId;

    /**
     * 好友用户ID
     */
    private Long friendUserId;

    /**
     * 备注名称
     */
    private String remarkName;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 权限范围（控制好友可见性等）
     */
    private Integer permissionScope;

    /**
     * 是否标星：0-否，1-是
     */
    private Integer isStarred;

    /**
     * 是否拉黑：0-否，1-是
     */
    private Integer isBlacklisted;

    /**
     * 是否删除：0-正常，1-已删除
     */
    private Integer isDeleted;
}
