package com.maxxvll.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 系统通知
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@TableName("sys_notification")
public class SysNotification {

    /**
     * 通知ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 发送者ID（系统通知为NULL）
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 目标类型：USER/GROUP/ALL
     */
    private String targetType;

    /**
     * 目标ID（单用户ID、群组ID或NULL表示全员）
     */
    private String targetId;

    /**
     * 关联业务ID
     */
    private String relatedId;

    /**
     * 关联业务类型
     */
    private String relatedType;

    /**
     * 优先级：0-普通，1-重要，2-紧急
     */
    private Integer priority;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 状态：0-未读，1-已读，2-已删除
     */
    private Integer status;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    // ==================== 常量定义 ====================

    /**
     * 通知类型
     */
    public static final String TYPE_FRIEND_APPLY = "FRIEND_APPLY";
    public static final String TYPE_GROUP_APPLY = "GROUP_APPLY";
    public static final String TYPE_GROUP_INVITE = "GROUP_INVITE";
    public static final String TYPE_MENTION = "MENTION";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_BROADCAST = "BROADCAST";

    /**
     * 目标类型
     */
    public static final String TARGET_USER = "USER";
    public static final String TARGET_GROUP = "GROUP";
    public static final String TARGET_ALL = "ALL";

    /**
     * 状态
     */
    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;
    public static final int STATUS_DELETED = 2;

    /**
     * 优先级
     */
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_IMPORTANT = 1;
    public static final int PRIORITY_URGENT = 2;

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && expireTime.before(new Date());
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return status != null && status == STATUS_DELETED;
    }

    public Date getCreatedAt() {
        return createTime;
    }

    public void setCreatedAt(Date createdAt) {
        this.createTime = createdAt;
    }

    public Date getUpdatedAt() {
        return updateTime;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updateTime = updatedAt;
    }
}
