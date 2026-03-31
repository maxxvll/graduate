package com.maxxvll.common.consumer;

import com.maxxvll.common.event.EmailEvent;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 邮件事件消费者
 * <p>
 * 从 Kafka 消费邮件事件，并通过 JavaMailSender 发送邮件
 * </p>
 *
 * <p><b>核心功能:</b></p>
 * <ul>
 *     <li>从 Kafka Topic 消费邮件事件</li>
 *     <li>通过 JavaMailSender 发送邮件</li>
 *     <li>记录发送日志（成功/失败）</li>
 *     <li>异常处理和重试</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Component
public class EmailEventConsumer extends BaseEventConsumer<EmailEvent> {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${app.kafka.features.email-async-enabled:true}")
    private boolean emailAsyncEnabled;

    /**
     * 消费邮件事件（单条消费）
     * <p>
     * 从 "email-event-topic" 消费邮件事件并发送
     * </p>
     */
    @KafkaListener(
        topics = "email-event-topic",
        groupId = "email-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEmailEvent(ConsumerRecord<String, EmailEvent> record) {
        if (!emailAsyncEnabled) {
            log.warn("邮件异步发送已禁用，跳过处理: eventId={}",
                record.value() != null ? record.value().getEventId() : "null");
            return;
        }

        consumeSingle(record, "email-event-topic");
    }

    /**
     * 批量消费邮件事件
     * <p>
     * 从 "email-event-topic" 批量消费邮件事件并发送
     * </p>
     */
    @KafkaListener(
        topics = "email-event-topic",
        groupId = "email-consumer-group",
        containerFactory = "batchKafkaListenerFactory"
    )
    public void handleEmailEventsBatch(List<ConsumerRecord<String, EmailEvent>> records, Acknowledgment ack) {
        if (!emailAsyncEnabled) {
            log.warn("邮件异步发送已禁用，跳过处理: count={}", records.size());
            if (ack != null) {
                ack.acknowledge();
            }
            return;
        }

        consumeBatch(records, ack, "email-event-topic");
    }

    @Override
    protected void processEvent(EmailEvent event) throws Exception {
        log.info("开始发送邮件: eventId={}, to={}, subject={}, emailType={}",
            event.getEventId(), event.getTo(), event.getSubject(), event.getEmailType());

        try {
            // 构建邮件消息
            SimpleMailMessage message = new SimpleMailMessage();

            // 设置发件人（优先使用事件中的发件人，否则使用默认发件人）
            message.setFrom(event.getFrom() != null ? event.getFrom() : mailFrom);
            message.setTo(event.getTo());
            message.setSubject(event.getSubject());

            // 设置邮件内容
            if (event.getContent() != null) {
                message.setText(event.getContent());
            } else {
                // 默认内容（如果事件中没有提供内容）
                message.setText(generateDefaultContent(event));
            }

            // 发送邮件
            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("邮件发送成功: eventId={}, to={}, emailType={}, costTime={}ms",
                event.getEventId(), event.getTo(), event.getEmailType(), costTime);

        } catch (Exception e) {
            log.error("邮件发送失败: eventId={}, to={}, emailType={}, error={}",
                event.getEventId(), event.getTo(), event.getEmailType(), e.getMessage(), e);

            // 重新抛出异常，让 BaseEventConsumer 处理
            throw e;
        }
    }

    @Override
    protected void handleProcessError(ConsumerRecord<String, EmailEvent> record, Exception e) {
        EmailEvent event = record.value();

        log.error("邮件发送失败（将不再重试）: eventId={}, to={}, subject={}, emailType={}, errorType={}, errorMessage={}",
            event != null ? event.getEventId() : "null",
            event != null ? event.getTo() : "null",
            event != null ? event.getSubject() : "null",
            event != null ? event.getEmailType() : "null",
            e.getClass().getSimpleName(),
            e.getMessage(),
            e);

        // 可以在此处添加：
        // 1. 发送到死信队列（DLQ）
        // 2. 发送告警通知
        // 3. 记录到数据库失败表
    }

    @Override
    protected boolean validateEvent(EmailEvent event) {
        if (event == null) {
            log.warn("邮件事件为空");
            return false;
        }

        if (event.getTo() == null || event.getTo().trim().isEmpty()) {
            log.warn("邮件收件人为空: eventId={}", event.getEventId());
            return false;
        }

        if (event.getSubject() == null || event.getSubject().trim().isEmpty()) {
            log.warn("邮件主题为空: eventId={}", event.getEventId());
            return false;
        }

        return true;
    }

    /**
     * 生成默认邮件内容
     * <p>
     * 当事件中没有提供内容时，根据邮件类型生成默认内容
     * </p>
     *
     * @param event 邮件事件
     * @return 默认邮件内容
     */
    private String generateDefaultContent(EmailEvent event) {
        if (event.getEmailType() == null) {
            return "您好！\n\n这是一封来自系统的邮件。\n\n如非本人操作，请忽略此邮件。";
        }

        switch (event.getEmailType()) {
            case REGISTER_CODE:
            case LOGIN_CODE:
                return String.format("您好！\n\n您的验证码为：%s\n\n验证码有效期5分钟，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。",
                    event.getVerificationCode() != null ? event.getVerificationCode() : "****");

            case FRIEND_APPLICATION:
                return "您好！\n\n您有一条新的好友申请，请登录应用查看详情。";

            case GROUP_INVITATION:
                return "您好！\n\n您收到了群组邀请，请登录应用查看详情。";

            case SYSTEM_NOTIFICATION:
            default:
                return "您好！\n\n这是一封来自系统的通知邮件。\n\n如非本人操作，请忽略此邮件。";
        }
    }
}
