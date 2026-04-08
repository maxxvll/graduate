package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 好友分组表
 * @TableName friend_group
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friend_group")
public class FriendGroup extends BaseEntity {

    /**
     * 分组ID（使用雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 拥有者用户ID
     */
    private Long ownerUserId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 排序（数字越小越靠前）
     */
    private Integer groupOrder;

    /**
     * 是否默认分组：0-否，1-是
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
