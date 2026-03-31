package com.maxxvll.common.constants;

/**
 * 通知相关常量
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public class NotificationConstants {

    // ==================== 通知类型 ====================

    /**
     * 好友申请
     */
    public static final String TYPE_FRIEND_APPLY = "FRIEND_APPLY";

    /**
     * 群申请
     */
    public static final String TYPE_GROUP_APPLY = "GROUP_APPLY";

    /**
     * 群邀请
     */
    public static final String TYPE_GROUP_INVITE = "GROUP_INVITE";

    /**
     * @提及
     */
    public static final String TYPE_MENTION = "MENTION";

    /**
     * 系统通知
     */
    public static final String TYPE_SYSTEM = "SYSTEM";

    /**
     * 全员通知
     */
    public static final String TYPE_BROADCAST = "BROADCAST";

    // ==================== 目标类型 ====================

    /**
     * 单个用户
     */
    public static final String TARGET_USER = "USER";

    /**
     * 群组
     */
    public static final String TARGET_GROUP = "GROUP";

    /**
     * 全员
     */
    public static final String TARGET_ALL = "ALL";

    // ==================== 通知状态 ====================

    /**
     * 未读
     */
    public static final int STATUS_UNREAD = 0;

    /**
     * 已读
     */
    public static final int STATUS_READ = 1;

    /**
     * 已删除
     */
    public static final int STATUS_DELETED = 2;

    // ==================== 优先级 ====================

    /**
     * 普通
     */
    public static final int PRIORITY_NORMAL = 0;

    /**
     * 重要
     */
    public static final int PRIORITY_IMPORTANT = 1;

    /**
     * 紧急
     */
    public static final int PRIORITY_URGENT = 2;

    // ==================== Kafka Topic ====================

    /**
     * 通知主题
     */
    public static final String TOPIC_NOTIFICATION = "notification-topic";

    /**
     * 系统通知主题
     */
    public static final String TOPIC_SYSTEM_NOTIFICATION = "system-notification-topic";

    private NotificationConstants() {
        // 私有构造函数，防止实例化
    }
}
