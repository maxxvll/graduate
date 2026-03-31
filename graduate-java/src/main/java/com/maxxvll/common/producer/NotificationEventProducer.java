package com.maxxvll.common.producer;

import com.maxxvll.common.constants.NotificationConstants;
import com.maxxvll.common.event.NotificationEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 通知事件生产者
 * <p>
 * 通过 Kafka 异步发送通知事件
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Component
public class NotificationEventProducer {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送通知事件
     *
     * @param event 通知事件
     */
    public void sendNotificationEvent(NotificationEvent event) {
        if (event == null) {
            log.warn("通知事件为空，跳过发送");
            return;
        }

        String topic = determineTopic(event);
        String key = determineKey(event);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("通知事件发送成功: topic={}, key={}, notificationType={}, targetId={}",
                        topic, key, event.getNotificationType(), event.getTargetId());
            } else {
                log.error("通知事件发送失败: topic={}, key={}, notificationType={}, error={}",
                        topic, key, event.getNotificationType(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * 发送通知事件（同步等待）
     *
     * @param event 通知事件
     * @return 是否发送成功
     */
    public boolean sendNotificationEventSync(NotificationEvent event) {
        if (event == null) {
            log.warn("通知事件为空，跳过发送");
            return false;
        }

        String topic = determineTopic(event);
        String key = determineKey(event);

        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, event).get();
            log.debug("通知事件发送成功: topic={}, key={}, partition={}, offset={}",
                    topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("通知事件发送失败: topic={}, key={}, error={}", topic, key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送好友申请通知
     */
    public void sendFriendApplyNotification(NotificationEvent event) {
        sendNotificationEvent(event);
    }

    /**
     * 发送群申请通知
     */
    public void sendGroupApplyNotification(NotificationEvent event) {
        sendNotificationEvent(event);
    }

    /**
     * 发送@提及通知
     */
    public void sendMentionNotification(NotificationEvent event) {
        sendNotificationEvent(event);
    }

    /**
     * 发送系统通知
     */
    public void sendSystemNotification(NotificationEvent event) {
        sendNotificationEvent(event);
    }

    /**
     * 发送全员通知
     */
    public void sendBroadcastNotification(NotificationEvent event) {
        sendNotificationEvent(event);
    }

    /**
     * 确定 Topic
     */
    private String determineTopic(NotificationEvent event) {
        if (NotificationConstants.TARGET_ALL.equals(event.getTargetType())) {
            return NotificationConstants.TOPIC_SYSTEM_NOTIFICATION;
        }
        return NotificationConstants.TOPIC_NOTIFICATION;
    }

    /**
     * 确定 Key（用于分区）
     */
    private String determineKey(NotificationEvent event) {
        if (event.getTargetId() != null) {
            return event.getTargetId();
        }
        if (NotificationConstants.TARGET_ALL.equals(event.getTargetType())) {
            return "broadcast";
        }
        return "unknown";
    }
}
