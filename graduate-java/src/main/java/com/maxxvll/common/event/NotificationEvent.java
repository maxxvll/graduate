package com.maxxvll.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

/**
 * 通知事件
 * <p>
 * 用于 Kafka 异步通知推送，支持多种通知类型
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent extends BaseKafkaEvent {

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
    private Long expireTime;

    // ==================== 通知类型常量 ====================

    public static final String TYPE_FRIEND_APPLY = "FRIEND_APPLY";
    public static final String TYPE_GROUP_APPLY = "GROUP_APPLY";
    public static final String TYPE_GROUP_INVITE = "GROUP_INVITE";
    public static final String TYPE_MENTION = "MENTION";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_BROADCAST = "BROADCAST";

    // ==================== 目标类型常量 ====================

    public static final String TARGET_USER = "USER";
    public static final String TARGET_GROUP = "GROUP";
    public static final String TARGET_ALL = "ALL";

    // ==================== 工厂方法 ====================

    @Override
    public String getEventType() {
        return "NOTIFICATION";
    }

    /**
     * 创建好友申请通知事件
     */
    public static NotificationEvent createFriendApplyEvent(String applicantId, String applicantName,
                                                          String applicantAvatar, String targetUserId,
                                                          Long applicationId, String remark) {
        return NotificationEvent.builder()
                .notificationType(TYPE_FRIEND_APPLY)
                .title("新的好友申请")
                .content(applicantName + " 申请添加你为好友" + (remark != null && !remark.isBlank() ? "：" + remark : ""))
                .senderId(applicantId)
                .senderName(applicantName)
                .senderAvatar(applicantAvatar)
                .targetType(TARGET_USER)
                .targetId(targetUserId)
                .relatedId(String.valueOf(applicationId))
                .relatedType("FRIEND_APPLICATION")
                .priority(0)
                .build();
    }

    /**
     * 创建群申请通知事件
     */
    public static NotificationEvent createGroupApplyEvent(String applicantId, String applicantName,
                                                         String applicantAvatar, Long groupId,
                                                         String groupName, Long applicationId) {
        return NotificationEvent.builder()
                .notificationType(TYPE_GROUP_APPLY)
                .title("新的入群申请")
                .content(applicantName + " 申请加入群聊【" + groupName + "】")
                .senderId(applicantId)
                .senderName(applicantName)
                .senderAvatar(applicantAvatar)
                .targetType(TARGET_USER)
                .targetId(null) // 需要根据群ID查找群主或管理员
                .relatedId(String.valueOf(applicationId))
                .relatedType("GROUP_APPLICATION")
                .priority(0)
                .build();
    }

    /**
     * 创建@提及通知事件
     */
    public static NotificationEvent createMentionEvent(String senderId, String senderName,
                                                      String senderAvatar, String sessionId,
                                                      String sessionType, Long messageId,
                                                      String mentionedUserId) {
        return NotificationEvent.builder()
                .notificationType(TYPE_MENTION)
                .title("@提及通知")
                .content(senderName + " 在" + (SessionType.GROUP.equals(sessionType) ? "群聊" : "会话") + "中@了你")
                .senderId(senderId)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .targetType(TARGET_USER)
                .targetId(mentionedUserId)
                .relatedId(String.valueOf(messageId))
                .relatedType("CHAT_MESSAGE")
                .priority(0)
                .build();
    }

    /**
     * 创建系统通知事件
     */
    public static NotificationEvent createSystemEvent(String title, String content,
                                                      String targetType, String targetId,
                                                      Integer priority) {
        return NotificationEvent.builder()
                .notificationType(TYPE_SYSTEM)
                .title(title)
                .content(content)
                .senderId(null)
                .senderName("系统通知")
                .senderAvatar(null)
                .targetType(targetType)
                .targetId(targetId)
                .relatedId(null)
                .relatedType(null)
                .priority(priority != null ? priority : 0)
                .build();
    }

    /**
     * 创建全员通知事件
     */
    public static NotificationEvent createBroadcastEvent(String title, String content, Integer priority) {
        return NotificationEvent.builder()
                .notificationType(TYPE_BROADCAST)
                .title(title)
                .content(content)
                .senderId(null)
                .senderName("系统公告")
                .senderAvatar(null)
                .targetType(TARGET_ALL)
                .targetId(null)
                .relatedId(null)
                .relatedType(null)
                .priority(priority != null ? priority : 1)
                .build();
    }

    // ==================== SessionType 占位引用 ====================

    /**
     * 会话类型（避免循环依赖）
     */
    public static class SessionType {
        public static final String GROUP = "GROUP";
        public static final String SINGLE = "SINGLE";
    }
}
