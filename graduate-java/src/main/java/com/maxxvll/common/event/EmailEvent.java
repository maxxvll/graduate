package com.maxxvll.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 邮件发送事件
 * <p>
 * 用于 Kafka 异步发送邮件，包括：
 * - 注册验证码邮件
 * - 登录验证码邮件
 * - 系统通知邮件
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EmailEvent extends BaseKafkaEvent {
    private static final long serialVersionUID = 1L;

    /**
     * 收件人邮箱地址
     */
    private String to;

    /**
     * 邮件主题
     */
    private String subject;

    /**
     * 邮件内容（纯文本）
     */
    private String content;

    /**
     * 邮件类型
     */
    private EmailType emailType;

    /**
     * 验证码（仅用于验证码类邮件）
     */
    private String verificationCode;

    /**
     * 发件人邮箱地址（可选，如果为空则使用默认发件人）
     */
    private String from;

    /**
     * 附加数据（JSON 格式，用于扩展）
     */
    private String metadata;

    @Override
    public String getEventType() {
        return "EMAIL";
    }

    @Override
    public String getEventDescription() {
        return String.format("[%s] eventId=%s, type=%s, emailType=%s, to=%s",
            getSource(), getEventId(), getEventType(), emailType, to);
    }

    /**
     * 邮件类型枚举
     */
    public enum EmailType {
        /**
         * 注册验证码
         */
        REGISTER_CODE,

        /**
         * 登录验证码
         */
        LOGIN_CODE,

        /**
         * 系统通知
         */
        SYSTEM_NOTIFICATION,

        /**
         * 好友申请通知
         */
        FRIEND_APPLICATION,

        /**
         * 群组邀请通知
         */
        GROUP_INVITATION
    }
}
