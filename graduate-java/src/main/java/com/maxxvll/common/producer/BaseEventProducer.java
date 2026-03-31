package com.maxxvll.common.producer;

import com.maxxvll.common.event.BaseKafkaEvent;
import com.maxxvll.common.logging.LogHelper;
import com.maxxvll.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Kafka 事件生产者基类
 * <p>
 * 提供统一的 Kafka 消息发送能力，所有事件生产者都应继承此类
 * </p>
 *
 * <p><b>核心功能:</b></p>
 * <ul>
 *     <li>异步发送消息，不阻塞业务线程</li>
 *     <li>统一日志记录（成功/失败）</li>
 *     <li>性能监控（发送耗时）</li>
 *     <li>错误处理和重试</li>
 * </ul>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Component
 * public class EmailEventProducer extends BaseEventProducer<EmailEvent> {
 *     private static final String TOPIC = "email-event-topic";
 *
 *     public void sendEmailEvent(EmailEvent event) {
 *         sendEvent(TOPIC, event);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 事件类型，必须继承 BaseKafkaEvent
 * @author Claude Code
 * @since 2026-03-17
 */
@Slf4j
public abstract class BaseEventProducer<T extends BaseKafkaEvent> {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 发送事件到 Kafka
     * <p>
     * 使用 eventId 作为消息键（Key），保证:
     * - 相同 eventId 的消息进入同一个分区（保证顺序性）
     * - 便于通过 eventId 追踪消息
     * </p>
     *
     * @param topic Kafka 主题名称
     * @param event 事件对象
     */
    protected void sendEvent(String topic, T event) {
        sendEvent(topic, event.getEventId(), event);
    }

    /**
     * 发送事件到 Kafka（自定义消息键）
     * <p>
     * 当需要使用自定义消息键时调用此方法
     * </p>
     *
     * @param topic Kafka 主题名称
     * @param key   消息键（用于分区路由）
     * @param event 事件对象
     */
    protected void sendEvent(String topic, String key, T event) {
        long startTime = System.currentTimeMillis();

        try {
            // 异步发送消息
            kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    if (ex == null) {
                        // 记录成功日志
                        LogHelper.logKafkaSend(topic, key, costTime, true);

                        log.debug("Kafka 消息发送成功: topic={}, key={}, eventId={}, partition={}, offset={}, costTime={}ms",
                            topic,
                            key,
                            event.getEventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            costTime);
                    } else {
                        // 记录失败日志
                        LogHelper.logKafkaSend(topic, key, costTime, false);

                        // 调用失败处理
                        handleSendFailure(topic, event, ex, costTime);
                    }
                });

            log.debug("Kafka 消息已提交: topic={}, key={}, eventType={}, eventId={}",
                topic, key, event.getEventType(), event.getEventId());

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaSend(topic, key, costTime, false);

            log.error("Kafka 消息提交异常: topic={}, key={}, eventId={}, error={}",
                topic, key, event.getEventId(), e.getMessage(), e);

            // 抛出业务异常
            throw new BusinessException("Kafka 消息发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步发送事件（阻塞等待结果）
     * <p>
     * <b>警告:</b> 此方法会阻塞线程，仅用于需要确认发送结果的场景
     * 大多数情况下应使用异步方法 {@link #sendEvent(String, BaseKafkaEvent)}
     * </p>
     *
     * @param topic Kafka 主题名称
     * @param event 事件对象
     * @return 是否发送成功
     */
    protected boolean sendEventSync(String topic, T event) {
        return sendEventSync(topic, event.getEventId(), event);
    }

    /**
     * 同步发送事件（阻塞等待结果，自定义消息键）
     *
     * @param topic Kafka 主题名称
     * @param key   消息键
     * @param event 事件对象
     * @return 是否发送成功
     */
    protected boolean sendEventSync(String topic, String key, T event) {
        long startTime = System.currentTimeMillis();

        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, event).get();
            long costTime = System.currentTimeMillis() - startTime;

            LogHelper.logKafkaSend(topic, key, costTime, true);

            log.info("Kafka 消息同步发送成功: topic={}, key={}, eventId={}, partition={}, offset={}, costTime={}ms",
                topic,
                key,
                event.getEventId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                costTime);

            return true;

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaSend(topic, key, costTime, false);

            log.error("Kafka 消息同步发送失败: topic={}, key={}, eventId={}, costTime={}ms, error={}",
                topic, key, event.getEventId(), costTime, e.getMessage(), e);

            handleSendFailure(topic, event, e, costTime);
            return false;
        }
    }

    /**
     * 带重试的异步发送
     * <p>
     * 当发送失败时，自动重试指定次数
     * </p>
     *
     * @param topic         Kafka 主题名称
     * @param event         事件对象
     * @param maxRetries    最大重试次数
     * @param retryDelayMs  重试延迟（毫秒）
     */
    protected void sendEventWithRetry(String topic, T event, int maxRetries, long retryDelayMs) {
        sendEventWithRetry(topic, event.getEventId(), event, maxRetries, retryDelayMs);
    }

    /**
     * 带重试的异步发送（自定义消息键）
     *
     * @param topic         Kafka 主题名称
     * @param key           消息键
     * @param event         事件对象
     * @param maxRetries    最大重试次数
     * @param retryDelayMs  重试延迟（毫秒）
     */
    protected void sendEventWithRetry(String topic, String key, T event, int maxRetries, long retryDelayMs) {
        int normalizedRetries = Math.max(1, maxRetries);
        sendEventWithRetryInternal(topic, key, event, normalizedRetries, retryDelayMs, 1);
    }

    private void sendEventWithRetryInternal(String topic,
                                            String key,
                                            T event,
                                            int maxRetries,
                                            long retryDelayMs,
                                            int attempt) {
        long startTime = System.currentTimeMillis();

        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        long costTime = System.currentTimeMillis() - startTime;
                        if (ex == null) {
                            LogHelper.logKafkaSend(topic, key, costTime, true);
                            log.info("Kafka 消息发送成功: topic={}, key={}, eventId={}, attempt={}, partition={}, offset={}, costTime={}ms",
                                    topic,
                                    key,
                                    event.getEventId(),
                                    attempt,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    costTime);
                            return;
                        }

                        LogHelper.logKafkaSend(topic, key, costTime, false);
                        retryOrFail(topic, key, event, maxRetries, retryDelayMs, attempt, ex, costTime);
                    });
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaSend(topic, key, costTime, false);
            retryOrFail(topic, key, event, maxRetries, retryDelayMs, attempt, e, costTime);
        }
    }

    private void retryOrFail(String topic,
                             String key,
                             T event,
                             int maxRetries,
                             long retryDelayMs,
                             int attempt,
                             Throwable ex,
                             long costTime) {
        if (attempt < maxRetries) {
            log.warn("Kafka 消息发送失败，准备异步重试: topic={}, key={}, eventId={}, attempt={}/{}, error={}",
                    topic, key, event.getEventId(), attempt, maxRetries, ex.getMessage());

            CompletableFuture.delayedExecutor(Math.max(0L, retryDelayMs), TimeUnit.MILLISECONDS)
                    .execute(() -> sendEventWithRetryInternal(topic, key, event, maxRetries, retryDelayMs, attempt + 1));
            return;
        }

        log.error("Kafka 消息发送失败，已达最大重试次数: topic={}, key={}, eventId={}, maxRetries={}, costTime={}ms",
                topic, key, event.getEventId(), maxRetries, costTime, ex);
        handleSendFailure(topic, event, ex, costTime);
    }

    /**
     * 处理发送失败
     * <p>
     * 子类可以重写此方法实现自定义的失败处理逻辑，例如：
     * - 发送到死信队列（DLQ）
     * - 发送告警通知
     * - 记录到数据库
     * </p>
     *
     * @param topic    Kafka 主题名称
     * @param event    事件对象
     * @param ex       异常对象
     * @param costTime 发送耗时（毫秒）
     */
    protected void handleSendFailure(String topic, T event, Throwable ex, long costTime) {
        log.error("Kafka 发送失败: topic={}, eventType={}, eventId={}, costTime={}ms, errorType={}, errorMessage={}",
            topic,
            event.getEventType(),
            event.getEventId(),
            costTime,
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            ex);

        // 默认处理：记录详细日志
        // 子类可以重写此方法，实现：
        // 1. 发送到死信队列
        // 2. 发送告警通知
        // 3. 记录到数据库失败表
    }
}
