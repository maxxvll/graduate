package com.maxxvll.common.producer;

import com.maxxvll.common.event.FriendApplicationEvent;
import org.springframework.stereotype.Component;

/**
 * 好友申请事件生产者
 * <p>
 * 负责将好友申请相关事件发送到 Kafka Topic，实现异步通知推送
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Resource
 * private FriendApplicationEventProducer friendApplicationEventProducer;
 *
 * // 创建申请通知
 * FriendApplicationEvent event = FriendApplicationEvent.builder()
 *     .applicationId(application.getId())
 *     .applicantId(applicantId)
 *     .applicantUsername(applicant.getUsername())
 *     .targetUserId(targetUserId)
 *     .actionType(FriendApplicationEvent.ActionType.APPLY)
 *     .remark(remark)
 *     .createTime(new Date())
 *     .build();
 * friendApplicationEventProducer.sendFriendApplicationEvent(event);
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Component
public class FriendApplicationEventProducer extends BaseEventProducer<FriendApplicationEvent> {

    private static final String TOPIC = "friend-application-topic";

    /**
     * 发送好友申请事件
     * <p>
     * 使用 eventId 作为消息键，保证消息顺序性
     * </p>
     *
     * @param event 好友申请事件
     */
    public void sendFriendApplicationEvent(FriendApplicationEvent event) {
        sendEvent(TOPIC, event);
    }

    /**
     * 发送好友申请事件（使用目标用户ID作为消息键）
     * <p>
     * 保证同一用户的通知进入同一个分区，保证顺序性
     * </p>
     *
     * @param event 好友申请事件
     */
    public void sendFriendApplicationEventWithTargetKey(FriendApplicationEvent event) {
        // 使用目标用户ID作为消息键，保证同一用户的通知有序
        String key = String.valueOf(event.getTargetUserId());
        sendEvent(TOPIC, key, event);
    }

    /**
     * 发送好友申请事件（使用申请人ID作为消息键）
     * <p>
     * 用于通知申请人的场景（如申请被拒绝）
     * </p>
     *
     * @param event 好友申请事件
     */
    public void sendFriendApplicationEventWithApplicantKey(FriendApplicationEvent event) {
        // 使用申请人ID作为消息键
        String key = String.valueOf(event.getApplicantId());
        sendEvent(TOPIC, key, event);
    }

    /**
     * 同步发送好友申请事件（等待确认）
     * <p>
     * <b>警告:</b> 此方法会阻塞线程，仅用于需要确认发送结果的场景
     * </p>
     *
     * @param event 好友申请事件
     * @return 是否发送成功
     */
    public boolean sendFriendApplicationEventSync(FriendApplicationEvent event) {
        return sendEventSync(TOPIC, event);
    }

    /**
     * 带重试的发送好友申请事件
     *
     * @param event        好友申请事件
     * @param maxRetries   最大重试次数
     * @param retryDelayMs 重试延迟（毫秒）
     */
    public void sendFriendApplicationEventWithRetry(FriendApplicationEvent event, int maxRetries, long retryDelayMs) {
        sendEventWithRetry(TOPIC, event, maxRetries, retryDelayMs);
    }
}
