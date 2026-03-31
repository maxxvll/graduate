package com.maxxvll.common.consumer;

import com.maxxvll.common.dto.SystemNotificationSendDTO;
import com.maxxvll.common.event.FriendApplicationEvent;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.service.NotificationService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 好友申请事件消费者
 * <p>
 * 从 Kafka 消费好友申请事件，并通过 WebSocket 推送给相关用户
 * </p>
 *
 * <p><b>核心功能:</b></p>
 * <ul>
 *     <li>从 Kafka Topic 消费好友申请事件</li>
 *     <li>通过 WebSocket 推送通知给目标用户</li>
 *     <li>支持三种动作：APPLY、ACCEPT、REJECT</li>
 *     <li>记录推送日志（成功/失败）</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Component
public class FriendApplicationEventConsumer extends BaseEventConsumer<FriendApplicationEvent> {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private NotificationService notificationService;

    @Value("${app.kafka.features.friend-notification-async-enabled:true}")
    private boolean friendNotificationAsyncEnabled;

    /**
     * 消费好友申请事件（单条消费）
     */
    @KafkaListener(
        topics = "friend-application-topic",
        groupId = "friend-application-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleFriendApplicationEvent(ConsumerRecord<String, FriendApplicationEvent> record) {
        if (!friendNotificationAsyncEnabled) {
            log.warn("好友申请通知异步化已禁用，跳过处理: eventId={}",
                record.value() != null ? record.value().getEventId() : "null");
            return;
        }

        consumeSingle(record, "friend-application-topic");
    }

    /**
     * 批量消费好友申请事件
     */
    @KafkaListener(
        topics = "friend-application-topic",
        groupId = "friend-application-consumer-group",
        containerFactory = "batchKafkaListenerFactory"
    )
    public void handleFriendApplicationEventsBatch(List<ConsumerRecord<String, FriendApplicationEvent>> records, Acknowledgment ack) {
        if (!friendNotificationAsyncEnabled) {
            log.warn("好友申请通知异步化已禁用，跳过处理: count={}", records.size());
            if (ack != null) {
                ack.acknowledge();
            }
            return;
        }

        consumeBatch(records, ack, "friend-application-topic");
    }

    @Override
    protected void processEvent(FriendApplicationEvent event) throws Exception {
        log.info("开始处理好友申请事件: eventId={}, actionType={}, applicationId={}",
            event.getEventId(), event.getActionType(), event.getApplicationId());

        // 根据动作类型决定通知谁
        Long targetUserId;
        switch (event.getActionType()) {
            case APPLY:
                // 申请创建：通知目标用户
                targetUserId = event.getTargetUserId();
                break;

            case ACCEPT:
            case REJECT:
                // 申请处理：通知申请人
                targetUserId = event.getApplicantId();
                break;

            default:
                log.warn("未知的好友申请动作类型: {}", event.getActionType());
                return;
        }

        // 1. 先保存通知到数据库（持久化）
        SystemNotificationSendDTO notificationDTO = buildNotificationDTO(event);
        try {
            notificationService.sendToUser(notificationDTO, String.valueOf(event.getApplicantId()),
                    event.getApplicantUsername(), event.getApplicantAvatar());
        } catch (Exception e) {
            log.error("保存好友申请通知失败: eventId={}, targetUserId={}", event.getEventId(), targetUserId, e);
        }

        // 2. 通过 WebSocket 推送实时通知
        Map<String, Object> wsNotification = buildWebSocketNotification(event);
        boolean success = pushNotification(String.valueOf(targetUserId), wsNotification);

        if (success) {
            log.info("好友申请通知推送成功: eventId={}, targetUserId={}, actionType={}",
                event.getEventId(), targetUserId, event.getActionType());
        } else {
            log.warn("好友申请通知推送失败（用户不在线）: eventId={}, targetUserId={}, actionType={}",
                event.getEventId(), targetUserId, event.getActionType());
        }
    }

    @Override
    protected void handleProcessError(ConsumerRecord<String, FriendApplicationEvent> record, Exception e) {
        FriendApplicationEvent event = record.value();

        log.error("好友申请事件处理失败: eventId={}, applicationId={}, actionType={}, errorType={}, errorMessage={}",
            event != null ? event.getEventId() : "null",
            event != null ? event.getApplicationId() : "null",
            event != null ? event.getActionType() : "null",
            e.getClass().getSimpleName(),
            e.getMessage(),
            e);
    }

    /**
     * 构建通知DTO（用于数据库持久化）
     */
    private SystemNotificationSendDTO buildNotificationDTO(FriendApplicationEvent event) {
        SystemNotificationSendDTO dto = new SystemNotificationSendDTO();
        dto.setNotificationType(SystemNotificationSendDTO.TYPE_FRIEND_APPLY);
        dto.setTargetType(SystemNotificationSendDTO.TARGET_USER);
        dto.setTargetId(String.valueOf(event.getTargetUserId()));
        dto.setRelatedId(String.valueOf(event.getApplicationId()));
        dto.setRelatedType("FRIEND_APPLICATION");

        // 根据动作类型设置标题和内容
        switch (event.getActionType()) {
            case APPLY:
                dto.setTitle("新的好友申请");
                dto.setContent(event.getApplicantUsername() + " 申请添加你为好友"
                        + (event.getRemark() != null && !event.getRemark().isBlank() ? "：" + event.getRemark() : ""));
                break;

            case ACCEPT:
                dto.setTitle("好友申请已通过");
                dto.setContent("您的好友申请已被 " + event.getApplicantUsername() + " 通过，现在可以开始聊天了");
                break;

            case REJECT:
                dto.setTitle("好友申请已拒绝");
                dto.setContent("您的好友申请已被 " + event.getApplicantUsername() + " 拒绝"
                        + (event.getRejectReason() != null && !event.getRejectReason().isBlank() ? "：" + event.getRejectReason() : ""));
                break;
        }

        return dto;
    }

    /**
     * 构建 WebSocket 通知消息
     *
     * @param event 好友申请事件
     * @return 通知消息
     */
    private Map<String, Object> buildWebSocketNotification(FriendApplicationEvent event) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "friend_application");
        notification.put("action", event.getActionType().name());
        notification.put("eventId", event.getEventId());

        // 申请基本信息
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", event.getApplicationId());
        data.put("applicantId", event.getApplicantId());
        data.put("applicantUsername", event.getApplicantUsername());
        data.put("applicantAvatar", event.getApplicantAvatar());
        data.put("remark", event.getRemark());
        data.put("createTime", event.getCreateTime());

        // 根据动作类型添加额外信息
        switch (event.getActionType()) {
            case ACCEPT:
                data.put("sessionId", event.getSessionId());
                data.put("message", "好友申请已通过");
                break;

            case REJECT:
                data.put("rejectReason", event.getRejectReason());
                data.put("message", "好友申请已拒绝");
                break;

            case APPLY:
                data.put("message", "您有一条新的好友申请");
                break;
        }

        notification.put("data", data);
        return notification;
    }

    /**
     * 通过 WebSocket 推送通知给用户
     *
     * @param userId       用户ID
     * @param notification 通知消息
     * @return 是否推送成功
     */
    private boolean pushNotification(String userId, Map<String, Object> notification) {
        try {
            Channel channel = nettyChannelManager.getChannel(userId);

            if (channel == null) {
                log.debug("用户 Channel 不存在，用户可能离线: userId={}", userId);
                return false;
            }

            if (!channel.isActive()) {
                log.warn("用户 Channel 已断开: userId={}", userId);
                nettyChannelManager.removeChannel(userId);
                return false;
            }

            // 将通知消息转换为 JSON 并发送
            String notificationJson = com.alibaba.fastjson2.JSON.toJSONString(notification);
            channel.writeAndFlush(new TextWebSocketFrame(notificationJson));

            log.debug("好友申请通知推送成功: userId={}, notification={}", userId, notification.get("action"));
            return true;

        } catch (Exception e) {
            log.error("好友申请通知推送失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected boolean validateEvent(FriendApplicationEvent event) {
        if (event == null) {
            log.warn("好友申请事件为空");
            return false;
        }

        if (event.getActionType() == null) {
            log.warn("好友申请事件动作类型为空: eventId={}", event.getEventId());
            return false;
        }

        if (event.getApplicantId() == null || event.getTargetUserId() == null) {
            log.warn("好友申请事件用户ID为空: eventId={}", event.getEventId());
            return false;
        }

        return true;
    }
}
