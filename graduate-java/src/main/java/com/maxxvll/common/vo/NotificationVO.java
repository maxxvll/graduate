package com.maxxvll.common.vo;

import lombok.Data;

import java.util.Date;

/**
 * 通知VO
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
public class NotificationVO {

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 通知类型描述
     */
    private String notificationTypeDesc;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 发送者ID
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
     * 优先级描述
     */
    private String priorityDesc;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 相对时间描述（如"5分钟前"）
     */
    private String timeDesc;
}
