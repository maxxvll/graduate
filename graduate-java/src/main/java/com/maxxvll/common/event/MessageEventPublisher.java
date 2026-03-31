package com.maxxvll.common.event;

import com.maxxvll.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * 消息事件发布器
 * <p>
 * 用于发布消息相关的事件，解耦服务之间的依赖关系
 * 支持同步和异步两种发布模式
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 同步发布
 * messageEventPublisher.publishMessageSentEvent(message);
 *
 * // 异步发布
 * messageEventPublisher.publishMessageSentEventAsync(message);
 * }</pre>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Slf4j
@Component
public class MessageEventPublisher {

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource(name = "applicationEventMulticaster")
    private ApplicationEventMulticaster eventMulticaster;

    // ==================== 同步发布方法 ====================

    /**
     * 发布消息发送后事件
     * 用于触发会话更新
     */
    public void publishMessageSentEvent(ChatMessage message) {
        SessionUpdateEvent event = SessionUpdateEvent.builder()
                .source(this)
                .type(SessionUpdateEvent.SessionUpdateType.MESSAGE_SENT)
                .message(message)
                .sessionId(message.getSessionId())
                .sessionType(message.getSessionType())
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布消息发送事件: messageId={}, sessionId={}", message.getId(), message.getSessionId());
    }

    /**
     * 发布消息撤回后事件
     * 用于触发会话更新
     */
    public void publishMessageRevokedEvent(ChatMessage message) {
        SessionUpdateEvent event = SessionUpdateEvent.builder()
                .source(this)
                .type(SessionUpdateEvent.SessionUpdateType.MESSAGE_REVOKED)
                .message(message)
                .sessionId(message.getSessionId())
                .sessionType(message.getSessionType())
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布消息撤回事件: messageId={}, sessionId={}", message.getId(), message.getSessionId());
    }

    /**
     * 发布消息编辑后事件
     * 用于触发会话更新和WebSocket推送
     */
    public void publishMessageDeletedEvent(ChatMessage message) {
        publishMessageRevokedEvent(message);
    }

    public void publishMessageEditedEvent(ChatMessage message) {
        SessionUpdateEvent event = SessionUpdateEvent.builder()
                .source(this)
                .type(SessionUpdateEvent.SessionUpdateType.MESSAGE_EDITED)
                .message(message)
                .sessionId(message.getSessionId())
                .sessionType(message.getSessionType())
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布消息编辑事件: messageId={}, sessionId={}", message.getId(), message.getSessionId());
    }

    /**
     * 发布清除未读数事件
     */
    public void publishClearUnreadEvent(String sessionId, String userId) {
        SessionUpdateEvent event = SessionUpdateEvent.builder()
                .source(this)
                .type(SessionUpdateEvent.SessionUpdateType.CLEAR_UNREAD)
                .sessionId(sessionId)
                .userId(userId)
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布清除未读数事件: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * 发布刷新最后一条消息事件
     */
    public void publishRefreshLastMessageEvent(ChatMessage message) {
        SessionUpdateEvent event = SessionUpdateEvent.builder()
                .source(this)
                .type(SessionUpdateEvent.SessionUpdateType.REFRESH_LAST_MESSAGE)
                .message(message)
                .sessionId(message.getSessionId())
                .sessionType(message.getSessionType())
                .build();

        eventPublisher.publishEvent(event);
        log.debug("发布刷新最后消息事件: messageId={}, sessionId={}", message.getId(), message.getSessionId());
    }

    // ==================== 异步发布方法 ====================

    /**
     * 异步发布消息发送事件
     *
     * @param message 消息
     * @return CompletableFuture
     */
    @Async("appTaskExecutor")
    public CompletableFuture<Void> publishMessageSentEventAsync(ChatMessage message) {
        return CompletableFuture.runAsync(() -> {
            publishMessageSentEvent(message);
        });
    }

    /**
     * 异步发布消息撤回事件
     *
     * @param message 消息
     * @return CompletableFuture
     */
    @Async("appTaskExecutor")
    public CompletableFuture<Void> publishMessageRevokedEventAsync(ChatMessage message) {
        return CompletableFuture.runAsync(() -> {
            publishMessageRevokedEvent(message);
        });
    }

    /**
     * 异步发布消息编辑事件
     *
     * @param message 消息
     * @return CompletableFuture
     */
    @Async("appTaskExecutor")
    public CompletableFuture<Void> publishMessageEditedEventAsync(ChatMessage message) {
        return CompletableFuture.runAsync(() -> {
            publishMessageEditedEvent(message);
        });
    }

    /**
     * 异步发布清除未读数事件
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return CompletableFuture
     */
    @Async("appTaskExecutor")
    public CompletableFuture<Void> publishClearUnreadEventAsync(String sessionId, String userId) {
        return CompletableFuture.runAsync(() -> {
            publishClearUnreadEvent(sessionId, userId);
        });
    }

    // ==================== 统一事件发布接口 ====================

    /**
     * 发布任意事件（同步）
     *
     * @param event 事件对象
     */
    public void publishEvent(Object event) {
        eventPublisher.publishEvent(event);
        log.debug("发布事件: eventType={}", event.getClass().getSimpleName());
    }

    /**
     * 发布任意事件（异步）
     *
     * @param event 事件对象
     * @return CompletableFuture
     */
    @Async("appTaskExecutor")
    public CompletableFuture<Void> publishEventAsync(Object event) {
        return CompletableFuture.runAsync(() -> {
            publishEvent(event);
        });
    }

    /**
     * 发布带多播的事件（可同时通知多个监听器）
     *
     * @param event 事件对象
     */
    public void multicastEvent(Object event) {
        eventMulticaster.multicastEvent((org.springframework.context.ApplicationEvent) event);
        log.debug("多播事件: eventType={}", event.getClass().getSimpleName());
    }
}
