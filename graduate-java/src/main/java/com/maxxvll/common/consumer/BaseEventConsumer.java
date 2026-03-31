package com.maxxvll.common.consumer;

import com.maxxvll.common.annotation.PerformanceMonitor;
import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.event.BaseKafkaEvent;
import com.maxxvll.common.logging.LogHelper;
import com.maxxvll.common.logging.MdcHelper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

/**
 * Kafka 事件消费者基类
 * <p>
 * 提供统一的 Kafka 消息消费能力，所有事件消费者都应继承此类
 * </p>
 *
 * <p><b>核心功能:</b></p>
 * <ul>
 *     <li>批量消费消息，提升吞吐量</li>
 *     <li>自动传播 traceId（链路追踪）</li>
 *     <li>统一日志记录（成功/失败）</li>
 *     <li>性能监控（消费耗时）</li>
 *     <li>错误处理和统计</li>
 * </ul>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Component
 * public class EmailEventConsumer extends BaseEventConsumer<EmailEvent> {
 *
 *     @Override
 *     @PerformanceMonitor(warnThresholdMs = 3000)
 *     protected void processEvent(EmailEvent event) {
 *         // 处理邮件发送逻辑
 *         emailService.sendEmail(event.getTo(), event.getSubject(), event.getContent());
 *     }
 *
 *     @KafkaListener(topics = "email-event-topic", groupId = "email-consumer-group")
 *     public void handleEmailEvents(List<ConsumerRecord<String, EmailEvent>> records, Acknowledgment ack) {
 *         consumeBatch(records, ack, "email-event-topic");
 *     }
 * }
 * }</pre>
 *
 * @param <T> 事件类型，必须继承 BaseKafkaEvent
 * @author Claude Code
 * @since 2026-03-17
 */
public abstract class BaseEventConsumer<T extends BaseKafkaEvent> {

    // Protected logger field for subclasses to use
    // Note: Using instance field so getClass() works correctly for each subclass
    protected final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 批量消费消息
     * <p>
     * 此方法封装了批量消费的核心逻辑：
     * - 遍历所有消息记录
     * - 为每条消息设置 MDC 上下文（传播 traceId）
     * - 调用子类实现的 processEvent 方法
     * - 统计成功/失败数量
     * - 记录消费日志
     * - 手动提交 offset（如果提供了 Acknowledgment）
     * </p>
     *
     * @param records Kafka 消息记录列表
     * @param ack     手动提交确认对象（可为 null）
     * @param topic   Kafka 主题名称（用于日志）
     */
    protected void consumeBatch(List<ConsumerRecord<String, T>> records, Acknowledgment ack, String topic) {
        if (records == null || records.isEmpty()) {
            log.debug("消费批次为空: topic={}", topic);
            return;
        }

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;

        log.info("开始批量消费: topic={}, partition={}, offset={}, count={}",
            topic,
            records.get(0).partition(),
            records.get(0).offset(),
            records.size());

        for (ConsumerRecord<String, T> record : records) {
            try {
                T event = record.value();

                // 设置 MDC 上下文（传播上游的 traceId）
                if (event != null && event.getTraceId() != null) {
                    MdcHelper.setTraceId(event.getTraceId());
                }

                // 调用子类实现的处理逻辑
                processEvent(event);
                successCount++;

            } catch (Exception e) {
                failureCount++;

                // 调用错误处理
                handleProcessError(record, e);

            } finally {
                // 清理 MDC 上下文，避免内存泄漏
                MdcHelper.clearContext();
            }
        }

        // 手动提交 offset（如果提供了 Acknowledgment）
        if (ack != null) {
            ack.acknowledge();
        }

        long costTime = System.currentTimeMillis() - startTime;

        // 记录批量消费完成日志
        log.info("批量消费完成: topic={}, count={}, success={}, failure={}, costTime={}ms",
            topic, records.size(), successCount, failureCount, costTime);

        // 如果有失败的消息，记录警告
        if (failureCount > 0) {
            log.warn("批量消费存在失败: topic={}, success={}, failure={}, failureRate={}%",
                topic, successCount, failureCount,
                String.format("%.2f", (failureCount * 100.0 / records.size())));
        }
    }

    /**
     * 单条消费（兼容旧版本）
     * <p>
     * 当不使用批量消费时，可以使用此方法
     * </p>
     *
     * @param record Kafka 消息记录
     * @param topic  Kafka 主题名称（用于日志）
     */
    protected void consumeSingle(ConsumerRecord<String, T> record, String topic) {
        if (record == null) {
            log.warn("消费记录为空: topic={}", topic);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            T event = record.value();

            // 设置 MDC 上下文
            if (event != null && event.getTraceId() != null) {
                MdcHelper.setTraceId(event.getTraceId());
            }

            log.info("收到单条消息: topic={}, partition={}, offset={}, eventId={}, eventType={}",
                topic,
                record.partition(),
                record.offset(),
                event != null ? event.getEventId() : "null",
                event != null ? event.getEventType() : "null");

            // 调用子类实现的处理逻辑
            processEvent(event);

            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaConsume(topic, record.partition(), record.offset(), costTime);

            log.debug("消息处理成功: topic={}, eventId={}, costTime={}ms",
                topic, event != null ? event.getEventId() : "null", costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            LogHelper.logKafkaConsumeException(topic, record.partition(), record.offset(), e);

            // 调用错误处理
            handleProcessError(record, e);

        } finally {
            // 清理 MDC 上下文
            MdcHelper.clearContext();
        }
    }

    /**
     * 处理事件（抽象方法，子类必须实现）
     * <p>
     * 此方法是业务逻辑的核心处理方法
     * 建议添加 {@link PerformanceMonitor} 注解进行性能监控
     * </p>
     *
     * <p><b>注意事项:</b></p>
     * <ul>
     *     <li>此方法会被性能监控，注意处理耗时</li>
     *     <li>如果抛出异常，会被 handleProcessError 捕获</li>
     *     <li>不应在此方法中阻塞或进行长时间计算</li>
     * </ul>
     *
     * @param event 事件对象
     * @throws Exception 处理失败时抛出异常
     */
    @PerformanceMonitor(warnThresholdMs = LoggingConstants.KAFKA_CONSUME_THRESHOLD_MS)
    protected abstract void processEvent(T event) throws Exception;

    /**
     * 处理消费错误
     * <p>
     * 当 processEvent 抛出异常时，调用此方法
     * 子类可以重写此方法实现自定义的错误处理逻辑，例如：
     * - 发送到死信队列（DLQ）
     * - 发送告警通知
     * - 记录到数据库错误表
     * - 重试机制
     * </p>
     *
     * <p><b>默认行为:</b></p>
     * <ul>
     *     <li>记录详细的错误日志</li>
     *     <li>包含事件信息、Kafka 元数据、异常堆栈</li>
     * </ul>
     *
     * @param record Kafka 消息记录
     * @param e      异常对象
     */
    protected void handleProcessError(ConsumerRecord<String, T> record, Exception e) {
        T event = record.value();

        log.error("消息处理失败: topic={}, partition={}, offset={}, eventId={}, eventType={}, errorType={}, errorMessage={}",
            record.topic(),
            record.partition(),
            record.offset(),
            event != null ? event.getEventId() : "null",
            event != null ? event.getEventType() : "null",
            e.getClass().getSimpleName(),
            e.getMessage(),
            e);

        // 默认处理：记录详细错误日志
        // 子类可以重写此方法，实现：
        // 1. 发送到死信队列
        // 2. 发送告警通知
        // 3. 记录到数据库错误表
        // 4. 自动重试
    }

    /**
     * 验证事件（可选）
     * <p>
     * 在处理事件前进行校验，子类可以重写此方法
     * </p>
     *
     * @param event 事件对象
     * @return 校验是否通过
     */
    protected boolean validateEvent(T event) {
        // 默认实现：检查事件是否为空
        return event != null;
    }

    /**
     * 获取事件描述（用于日志）
     * <p>
     * 子类可以重写此方法提供更详细的事件描述
     * </p>
     *
     * @param event 事件对象
     * @return 事件描述字符串
     */
    protected String getEventDescription(T event) {
        if (event == null) {
            return "null";
        }
        return String.format("eventId=%s, eventType=%s, timestamp=%d",
            event.getEventId(),
            event.getEventType(),
            event.getTimestamp());
    }
}
