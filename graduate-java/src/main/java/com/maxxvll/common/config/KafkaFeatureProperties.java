package com.maxxvll.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka 功能特性开关配置
 * <p>
 * 通过配置文件控制 Kafka 异步功能的启用状态，支持灰度发布和快速回滚
 * </p>
 *
 * <p><b>配置示例（application.yaml）:</b></p>
 * <pre>{@code
 * app:
 *   kafka:
 *     features:
 *       # 高优先级功能（默认启用）
 *       email-async-enabled: true
 *       friend-notification-async-enabled: true
 *       audit-log-async-enabled: true
 *
 *       # 中优先级功能（默认启用）
 *       group-notification-async-enabled: true
 *       file-upload-log-async-enabled: true
 *
 *       # 低优先级功能（默认关闭，灰度发布）
 *       message-recall-async-enabled: false
 *       user-profile-update-async-enabled: false
 * }</pre>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Resource
 * private KafkaFeatureProperties kafkaFeatureProperties;
 *
 * public void sendEmailCode(String email) {
 *     if (kafkaFeatureProperties.isEmailAsyncEnabled()) {
 *         // 使用 Kafka 异步发送
 *         emailEventProducer.sendEmailEvent(event);
 *     } else {
 *         // 使用原有同步逻辑（回滚方案）
 *         mailSender.send(message);
 *     }
 * }
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.kafka.features")
public class KafkaFeatureProperties {

    // ==================== 高优先级功能（默认启用）====================

    /**
     * 邮件发送异步化开关
     * <p>
     * 启用后，邮件发送通过 Kafka 异步处理，响应时间从 3000ms → 50ms
     * </p>
     */
    private boolean emailAsyncEnabled = true;

    /**
     * 好友申请通知异步化开关
     * <p>
     * 启用后，好友申请通知通过 Kafka 异步推送
     * </p>
     */
    private boolean friendNotificationAsyncEnabled = true;

    /**
     * 系统审计日志异步化开关
     * <p>
     * 启用后，审计日志通过 Kafka 异步记录
     * </p>
     */
    private boolean auditLogAsyncEnabled = true;

    // ==================== 中优先级功能（默认启用）====================

    /**
     * 群组申请通知异步化开关
     * <p>
     * 启用后，群组申请通知通过 Kafka 异步推送
     * </p>
     */
    private boolean groupNotificationAsyncEnabled = true;

    /**
     * 文件上传日志异步化开关
     * <p>
     * 启用后，文件上传日志通过 Kafka 异步记录
     * </p>
     */
    private boolean fileUploadLogAsyncEnabled = true;

    // ==================== 低优先级功能（默认关闭，灰度发布）====================

    /**
     * 消息撤回通知异步化开关
     * <p>
     * 启用后，消息撤回通知通过 Kafka 异步处理（替代 Spring Event）
     * </p>
     */
    private boolean messageRecallAsyncEnabled = false;

    /**
     * 用户资料更新通知异步化开关
     * <p>
     * 启用后，用户资料更新通知通过 Kafka 异步同步
     * </p>
     */
    private boolean userProfileUpdateAsyncEnabled = false;

    // ==================== 全局开关 ====================

    /**
     * 全局 Kafka 功能开关
     * <p>
     * 当设置为 false 时，所有 Kafka 异步功能都会被禁用，自动降级为同步逻辑
     * </p>
     * <p>
     * <b>紧急回滚场景:</b> 当 Kafka 集群故障或出现严重问题时，可快速关闭所有 Kafka 功能
     * </p>
     */
    private boolean globalKafkaEnabled = true;

    // ==================== 辅助方法 ====================

    /**
     * 检查邮件发送异步化是否启用
     * <p>
     * 综合检查全局开关和功能开关
     * </p>
     *
     * @return 是否启用
     */
    public boolean isEmailAsyncEnabled() {
        return globalKafkaEnabled && emailAsyncEnabled;
    }

    /**
     * 检查好友申请通知异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isFriendNotificationAsyncEnabled() {
        return globalKafkaEnabled && friendNotificationAsyncEnabled;
    }

    /**
     * 检查系统审计日志异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isAuditLogAsyncEnabled() {
        return globalKafkaEnabled && auditLogAsyncEnabled;
    }

    /**
     * 检查群组申请通知异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isGroupNotificationAsyncEnabled() {
        return globalKafkaEnabled && groupNotificationAsyncEnabled;
    }

    /**
     * 检查文件上传日志异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isFileUploadLogAsyncEnabled() {
        return globalKafkaEnabled && fileUploadLogAsyncEnabled;
    }

    /**
     * 检查消息撤回通知异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isMessageRecallAsyncEnabled() {
        return globalKafkaEnabled && messageRecallAsyncEnabled;
    }

    /**
     * 检查用户资料更新通知异步化是否启用
     *
     * @return 是否启用
     */
    public boolean isUserProfileUpdateAsyncEnabled() {
        return globalKafkaEnabled && userProfileUpdateAsyncEnabled;
    }

    /**
     * 获取功能状态摘要（用于监控和日志）
     *
     * @return 功能状态摘要字符串
     */
    public String getStatusSummary() {
        return String.format(
            "KafkaFeatureStatus{global=%s, email=%s, friend=%s, audit=%s, group=%s, fileUpload=%s, recall=%s, profile=%s}",
            globalKafkaEnabled,
            emailAsyncEnabled,
            friendNotificationAsyncEnabled,
            auditLogAsyncEnabled,
            groupNotificationAsyncEnabled,
            fileUploadLogAsyncEnabled,
            messageRecallAsyncEnabled,
            userProfileUpdateAsyncEnabled
        );
    }
}
