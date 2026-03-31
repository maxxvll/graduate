package com.maxxvll.common.event;

import com.maxxvll.common.logging.MdcHelper;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka 事件基类
 * <p>
 * 所有 Kafka 事件都应继承此类，提供统一的元数据和追踪能力
 * </p>
 *
 * <p><b>核心功能:</b></p>
 * <ul>
 *     <li>自动生成 eventId（UUID）用于消息去重和追踪</li>
 *     <li>自动记录 timestamp（事件时间戳）</li>
 *     <li>自动传播 traceId（链路追踪 ID）</li>
 *     <li>统一事件来源标识</li>
 * </ul>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class EmailEvent extends BaseKafkaEvent {
 *     private String to;
 *     private String subject;
 *     private String content;
 *
 *     @Override
 *     public String getEventType() {
 *         return "EMAIL";
 *     }
 * }
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-17
 */
@Getter
@Setter
public abstract class BaseKafkaEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识
     * <p>
     * 使用 UUID 自动生成，用于：
     * - 消息去重
     * - 消息追踪
     * - Kafka 消息键（Key）
     * </p>
     */
    private String eventId;

    /**
     * 事件时间戳（毫秒）
     * <p>
     * 记录事件创建时间，用于：
     * - 消息顺序性分析
     * - 消息延迟监控
     * - 数据审计
     * </p>
     */
    private Long timestamp;

    /**
     * 链路追踪 ID
     * <p>
     * 从 MDC 上下文获取，用于：
     * - 分布式追踪
     * - 跨服务调用链路分析
     * - 问题排查
     * </p>
     */
    private String traceId;

    /**
     * 事件来源标识
     * <p>
     * 标识事件产生的服务或模块，如：
     * - "graduate-im" - IM 应用
     * - "graduate-notification" - 通知服务
     * </p>
     */
    private String source;

    /**
     * 默认构造函数
     * <p>
     * 自动初始化事件元数据：
     * - eventId: 自动生成 UUID
     * - timestamp: 当前时间戳
     * - traceId: 从 MDC 获取（如果不存在则生成新的）
     * - source: "graduate-im"
     * </p>
     */
    protected BaseKafkaEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toEpochMilli();

        // 从 MDC 获取 traceId，如果不存在则生成新的
        String currentTraceId = MdcHelper.getTraceId();
        if (currentTraceId == null || currentTraceId.trim().isEmpty()) {
            this.traceId = MdcHelper.generateTraceId();
        } else {
            this.traceId = currentTraceId;
        }

        this.source = "graduate-im";
    }

    /**
     * 获取事件类型
     * <p>
     * 子类必须实现此方法，返回事件类型标识
     * 推荐使用大写字母，如 "EMAIL"、"FRIEND_APPLICATION"、"AUDIT_LOG"
     * </p>
     *
     * @return 事件类型标识
     */
    public abstract String getEventType();

    /**
     * 获取事件描述
     * <p>
     * 返回事件的详细描述，用于日志输出
     * 子类可以重写此方法提供更详细的描述
     * </p>
     *
     * @return 事件描述
     */
    public String getEventDescription() {
        return String.format("[%s] eventId=%s, type=%s, source=%s",
            getSource(), getEventId(), getEventType(), getSource());
    }
}
