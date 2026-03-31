package com.maxxvll.common.consumer;

import com.maxxvll.common.constants.NotificationConstants;
import com.maxxvll.common.dto.SystemNotificationSendDTO;
import com.maxxvll.common.event.NotificationEvent;
import com.maxxvll.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 通知事件消费者
 * <p>
 * 从 Kafka 消费通知事件，存储到数据库并通过 WebSocket 推送
 * </p>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Component
public class NotificationEventConsumer extends BaseEventConsumer<NotificationEvent> {

    @Resource
    private NotificationService notificationService;

    /**
     * 消费通知事件（单条消费）
     */
    @KafkaListener(
            topics = {NotificationConstants.TOPIC_NOTIFICATION, NotificationConstants.TOPIC_SYSTEM_NOTIFICATION},
            groupId = "notification-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleNotificationEvent(ConsumerRecord<String, NotificationEvent> record) {
        consumeSingle(record, "notification-topic");
    }

    /**
     * 批量消费通知事件
     */
    @KafkaListener(
            topics = {NotificationConstants.TOPIC_NOTIFICATION, NotificationConstants.TOPIC_SYSTEM_NOTIFICATION},
            groupId = "notification-consumer-group",
            containerFactory = "batchKafkaListenerFactory"
    )
    public void handleNotificationEventsBatch(List<ConsumerRecord<String, NotificationEvent>> records,
                                              Acknowledgment ack) {
        consumeBatch(records, ack, "notification-topic");
    }

    @Override
    protected void processEvent(NotificationEvent event) throws Exception {
        log.info("开始处理通知事件: eventId={}, notificationType={}, targetType={}, targetId={}",
                event.getEventId(), event.getNotificationType(), event.getTargetType(), event.getTargetId());

        // 根据目标类型处理通知
        SystemNotificationSendDTO dto = convertToDTO(event);

        switch (event.getTargetType()) {
            case NotificationConstants.TARGET_USER:
                notificationService.sendToUser(dto, event.getSenderId(), event.getSenderName(), event.getSenderAvatar());
                break;

            case NotificationConstants.TARGET_GROUP:
                notificationService.sendToGroup(dto, event.getSenderId(), event.getSenderName(), event.getSenderAvatar());
                break;

            case NotificationConstants.TARGET_ALL:
                notificationService.sendBroadcast(dto);
                break;

            default:
                log.warn("未知的目标类型: targetType={}", event.getTargetType());
        }

        log.info("通知事件处理完成: eventId={}, notificationType={}",
                event.getEventId(), event.getNotificationType());
    }

    @Override
    protected void handleProcessError(ConsumerRecord<String, NotificationEvent> record, Exception e) {
        NotificationEvent event = record.value();

        log.error("通知事件处理失败: eventId={}, notificationType={}, targetId={}, errorType={}, errorMessage={}",
                event != null ? event.getEventId() : "null",
                event != null ? event.getNotificationType() : "null",
                event != null ? event.getTargetId() : "null",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e);
    }

    @Override
    protected boolean validateEvent(NotificationEvent event) {
        if (event == null) {
            log.warn("通知事件为空");
            return false;
        }

        if (event.getNotificationType() == null || event.getNotificationType().isBlank()) {
            log.warn("通知事件类型为空: eventId={}", event.getEventId());
            return false;
        }

        if (event.getTitle() == null || event.getTitle().isBlank()) {
            log.warn("通知事件标题为空: eventId={}", event.getEventId());
            return false;
        }

        if (event.getContent() == null || event.getContent().isBlank()) {
            log.warn("通知事件内容为空: eventId={}", event.getEventId());
            return false;
        }

        if (event.getTargetType() == null || event.getTargetType().isBlank()) {
            log.warn("通知事件目标类型为空: eventId={}", event.getEventId());
            return false;
        }

        return true;
    }

    /**
     * 转换为通知发送DTO
     */
    private SystemNotificationSendDTO convertToDTO(NotificationEvent event) {
        SystemNotificationSendDTO dto = new SystemNotificationSendDTO();
        dto.setNotificationType(event.getNotificationType());
        dto.setTitle(event.getTitle());
        dto.setContent(event.getContent());
        dto.setTargetType(event.getTargetType());
        dto.setTargetId(event.getTargetId());
        dto.setRelatedId(event.getRelatedId());
        dto.setRelatedType(event.getRelatedType());
        dto.setPriority(event.getPriority());
        dto.setExpireTime(event.getExpireTime());
        return dto;
    }
}
