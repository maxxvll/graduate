package com.maxxvll.common.producer;

import com.maxxvll.common.event.EmailEvent;
import org.springframework.stereotype.Component;

/**
 * 邮件事件生产者
 * <p>
 * 负责将邮件发送事件发送到 Kafka Topic，实现异步邮件发送
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Resource
 * private EmailEventProducer emailEventProducer;
 *
 * // 发送注册验证码邮件
 * EmailEvent event = EmailEvent.builder()
 *     .to("user@example.com")
 *     .subject("注册验证码")
 *     .content("您的验证码为：" + code)
 *     .emailType(EmailEvent.EmailType.REGISTER_CODE)
 *     .verificationCode(code)
 *     .build();
 * emailEventProducer.sendEmailEvent(event);
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Component
public class EmailEventProducer extends BaseEventProducer<EmailEvent> {

    private static final String TOPIC = "email-event-topic";

    /**
     * 发送邮件事件
     * <p>
     * 使用 eventId 作为消息键，保证同一封邮件的消息进入同一个分区
     * </p>
     *
     * @param event 邮件事件
     */
    public void sendEmailEvent(EmailEvent event) {
        sendEvent(TOPIC, event);
    }

    /**
     * 发送邮件事件（自定义消息键）
     * <p>
     * 当需要使用自定义消息键时调用此方法
     * </p>
     *
     * @param event 邮件事件
     * @param key   消息键（如收件人邮箱）
     */
    public void sendEmailEvent(EmailEvent event, String key) {
        sendEvent(TOPIC, key, event);
    }

    /**
     * 同步发送邮件事件（等待确认）
     * <p>
     * <b>警告:</b> 此方法会阻塞线程，仅用于需要确认发送结果的场景
     * </p>
     *
     * @param event 邮件事件
     * @return 是否发送成功
     */
    public boolean sendEmailEventSync(EmailEvent event) {
        return sendEventSync(TOPIC, event);
    }

    /**
     * 带重试的发送邮件事件
     *
     * @param event        邮件事件
     * @param maxRetries   最大重试次数
     * @param retryDelayMs 重试延迟（毫秒）
     */
    public void sendEmailEventWithRetry(EmailEvent event, int maxRetries, long retryDelayMs) {
        sendEventWithRetry(TOPIC, event, maxRetries, retryDelayMs);
    }
}
