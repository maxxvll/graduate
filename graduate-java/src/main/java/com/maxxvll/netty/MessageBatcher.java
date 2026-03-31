package com.maxxvll.netty;

import com.maxxvll.component.NettyChannelManager;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息批量处理器
 * <p>
 * 使用本地队列+批量发送减少网络往返，提升吞吐量
 * 适用于高并发场景下的消息发送优化
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 添加消息到批量队列
 * messageBatcher.addToBatch(userId, message);
 *
 * // 或直接发送（立即发送，不经过批量队列）
 * messageBatcher.sendImmediately(userId, message);
 * }</pre>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Slf4j
@Component
public class MessageBatcher {

    /**
     * 单批次最大消息数
     */
    @Value("${ws.batch.max-batch-size:50}")
    private int maxBatchSize;

    /**
     * 批量刷新间隔（毫秒）
     */
    @Value("${ws.batch.flush-interval-ms:100}")
    private long flushIntervalMs;

    /**
     * 启用批量处理
     */
    @Value("${ws.batch.enabled:true}")
    private boolean batchEnabled;

    @Resource
    private NettyChannelManager nettyChannelManager;

    /**
     * 用户待发送消息队列
     */
    private final Map<String, List<BatchMessage>> userMessageQueues = new ConcurrentHashMap<>();

    /**
     * 统计指标
     */
    private final AtomicLong totalBatchedMessages = new AtomicLong(0);
    private final AtomicLong totalBatchFlushes = new AtomicLong(0);
    private final AtomicLong totalBatchBytes = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("消息批量处理器初始化完成, enabled={}, maxBatchSize={}, flushIntervalMs={}",
                batchEnabled, maxBatchSize, flushIntervalMs);
    }

    /**
     * 添加消息到批量队列
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public void addToBatch(String userId, String message) {
        if (!batchEnabled || userId == null || message == null) {
            // 批量禁用或参数无效，直接发送
            sendImmediately(userId, message);
            return;
        }

        List<BatchMessage> queue = userMessageQueues.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (queue) {
            queue.add(new BatchMessage(message, System.currentTimeMillis()));

            // 达到批量阈值，立即刷新
            if (queue.size() >= maxBatchSize) {
                flushUserQueue(userId, queue);
            }
        }
    }

    /**
     * 批量添加多个消息
     *
     * @param userId   用户ID
     * @param messages 消息列表
     */
    public void addAllToBatch(String userId, List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        if (!batchEnabled) {
            // 批量禁用，逐条发送
            for (String message : messages) {
                sendImmediately(userId, message);
            }
            return;
        }

        List<BatchMessage> queue = userMessageQueues.computeIfAbsent(userId, k -> new ArrayList<>());
        synchronized (queue) {
            for (String message : messages) {
                if (message != null) {
                    queue.add(new BatchMessage(message, System.currentTimeMillis()));
                }
            }

            // 达到批量阈值，立即刷新
            if (queue.size() >= maxBatchSize) {
                flushUserQueue(userId, queue);
            }
        }
    }

    /**
     * 立即发送消息（不经过批量队列）
     *
     * @param userId  用户ID
     * @param message 消息内容
     */
    public void sendImmediately(String userId, String message) {
        if (userId == null || message == null) {
            return;
        }

        Channel channel = nettyChannelManager.getChannel(userId);
        if (channel == null || !channel.isActive()) {
            log.debug("Channel not available for user {}, message queued for retry", userId);
            return;
        }

        try {
            channel.writeAndFlush(new TextWebSocketFrame(message));
            totalBatchedMessages.incrementAndGet();
            totalBatchBytes.addAndGet(message.getBytes().length);
        } catch (Exception e) {
            log.error("Failed to send message to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 定时刷新所有用户队列（默认每100ms执行一次）
     */
    @Scheduled(fixedRateString = "${ws.batch.flush-interval-ms:100}")
    public void flushAllQueues() {
        if (!batchEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        totalBatchFlushes.incrementAndGet();

        // 遍历所有用户队列
        userMessageQueues.forEach((userId, queue) -> {
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    // 检查是否应该刷新（队列不为空）
                    BatchMessage oldest = queue.get(0);
                    if (now - oldest.enqueueTime >= flushIntervalMs || queue.size() >= maxBatchSize) {
                        flushUserQueue(userId, queue);
                    }
                }
            }
        });
    }

    /**
     * 刷新单个用户的队列
     */
    private void flushUserQueue(String userId, List<BatchMessage> queue) {
        if (queue.isEmpty()) {
            return;
        }

        Channel channel = nettyChannelManager.getChannel(userId);
        if (channel == null || !channel.isActive()) {
            log.debug("Channel not available for user {}, pending {} messages", userId, queue.size());
            return;
        }

        try {
            // 将队列中的消息合并为批量消息
            StringBuilder combined = new StringBuilder();
            combined.append("{\"type\":\"batch\",\"messages\":[");
            boolean first = true;
            for (BatchMessage msg : queue) {
                if (!first) {
                    combined.append(",");
                }
                combined.append(msg.content);
                first = false;
                totalBatchBytes.addAndGet(msg.content.getBytes().length);
            }
            combined.append("]}");

            channel.writeAndFlush(new TextWebSocketFrame(combined.toString()));
            totalBatchedMessages.addAndGet(queue.size());
            queue.clear();

            log.debug("Flushed {} messages to user {}", queue.size(), userId);
        } catch (Exception e) {
            log.error("Failed to flush batch to user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalBatchedMessages", totalBatchedMessages.get());
        stats.put("totalBatchFlushes", totalBatchFlushes.get());
        stats.put("totalBatchBytes", totalBatchBytes.get());
        stats.put("activeQueues", userMessageQueues.size());
        stats.put("pendingMessages", userMessageQueues.values().stream()
                .mapToInt(List::size)
                .sum());
        stats.put("batchEnabled", batchEnabled);
        return stats;
    }

    /**
     * 清理用户队列（用户离线时调用）
     */
    public void clearUserQueue(String userId) {
        List<BatchMessage> removed = userMessageQueues.remove(userId);
        if (removed != null && !removed.isEmpty()) {
            log.debug("Cleared {} pending messages for offline user {}", removed.size(), userId);
        }
    }

    /**
     * 批量消息记录
     */
    private record BatchMessage(String content, long enqueueTime) {}

    @PreDestroy
    public void shutdown() {
        log.info("消息批量处理器关闭，开始刷新剩余消息...");
        // 关闭前刷新所有队列
        userMessageQueues.forEach((userId, queue) -> {
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    flushUserQueue(userId, queue);
                }
            }
        });
        log.info("消息批量处理器已关闭");
    }
}
