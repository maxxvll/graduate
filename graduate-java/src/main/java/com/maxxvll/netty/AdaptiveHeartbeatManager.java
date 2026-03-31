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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自适应心跳管理器
 * <p>
 * 根据网络状况动态调整心跳间隔：
 * - 网络良好时：延长心跳间隔，降低资源消耗
 * - 网络不稳定时：缩短心跳间隔，快速检测连接状态
 * </p>
 *
 * <p><b>心跳策略:</b></p>
 * <ul>
 *   <li>默认间隔：30秒</li>
 *   <li>最小间隔：10秒（网络不稳定时）</li>
 *   <li>最大间隔：120秒（网络稳定时）</li>
 * </ul>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Slf4j
@Component
public class AdaptiveHeartbeatManager {

    /**
     * 默认心跳间隔（毫秒）
     */
    @Value("${ws.heartbeat.default-interval-ms:30000}")
    private long defaultIntervalMs;

    /**
     * 最小心跳间隔（毫秒）
     */
    @Value("${ws.heartbeat.min-interval-ms:10000}")
    private long minIntervalMs;

    /**
     * 最大心跳间隔（毫秒）
     */
    @Value("${ws.heartbeat.max-interval-ms:120000}")
    private long maxIntervalMs;

    /**
     * 判定网络稳定的连续成功次数
     */
    @Value("${ws.heartbeat.stable-threshold:5}")
    private int stableThreshold;

    /**
     * 判定网络不稳定的连续失败次数
     */
    @Value("${ws.heartbeat.unstable-threshold:3}")
    private int unstableThreshold;

    /**
     * 心跳超时时间（毫秒）
     */
    @Value("${ws.heartbeat.timeout-ms:10000}")
    private long timeoutMs;

    @Resource
    private NettyChannelManager nettyChannelManager;

    /**
     * 用户心跳状态
     */
    private final Map<String, HeartbeatState> heartbeatStates = new ConcurrentHashMap<>();

    /**
     * 全局统计
     */
    private final AtomicLong totalHeartbeatsSent = new AtomicLong(0);
    private final AtomicLong totalHeartbeatsReceived = new AtomicLong(0);
    private final AtomicLong totalHeartbeatsTimeout = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("自适应心跳管理器初始化完成, defaultInterval={}ms, min={}ms, max={}ms",
                defaultIntervalMs, minIntervalMs, maxIntervalMs);
    }

    /**
     * 定时发送心跳（根据各用户的自适应间隔）
     */
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeats() {
        long now = System.currentTimeMillis();

        heartbeatStates.forEach((userId, state) -> {
            // 检查是否需要发送心跳
            if (now - state.lastHeartbeatSent >= state.currentInterval) {
                sendHeartbeat(userId);
            }
        });
    }

    /**
     * 发送心跳
     */
    public void sendHeartbeat(String userId) {
        Channel channel = nettyChannelManager.getChannel(userId);
        if (channel == null || !channel.isActive()) {
            log.debug("Cannot send heartbeat to inactive channel, userId={}", userId);
            removeUser(userId);
            return;
        }

        HeartbeatState state = heartbeatStates.computeIfAbsent(userId,
                k -> new HeartbeatState(defaultIntervalMs));
        state.lastHeartbeatSent = System.currentTimeMillis();
        state.pending = true;

        try {
            String heartbeatMsg = "{\"type\":\"heartbeat\",\"action\":\"ping\",\"timestamp\":" + state.lastHeartbeatSent + "}";
            channel.writeAndFlush(new TextWebSocketFrame(heartbeatMsg));
            totalHeartbeatsSent.incrementAndGet();
            nettyChannelManager.recordHeartbeatSent(userId);
            log.trace("Sent heartbeat to user {}, interval={}ms", userId, state.currentInterval);
        } catch (Exception e) {
            log.error("Failed to send heartbeat to user {}: {}", userId, e.getMessage());
            handleHeartbeatFailure(userId);
        }
    }

    /**
     * 处理心跳响应
     *
     * @param userId 用户ID
     * @param clientTimestamp 客户端时间戳
     */
    public void handleHeartbeatResponse(String userId, long clientTimestamp) {
        HeartbeatState state = heartbeatStates.get(userId);
        if (state == null) {
            return;
        }

        state.pending = false;
        long now = System.currentTimeMillis();
        long roundTripTime = now - clientTimestamp;

        state.consecutiveSuccess++;
        state.consecutiveFailure = 0;

        // 更新响应时间统计
        if (state.responseTimes.size() >= 10) {
            state.responseTimes.poll();
        }
        state.responseTimes.add(roundTripTime);

        // 计算平均响应时间
        double avgResponseTime = state.responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        // 自适应调整间隔（每稳定5次后延长）
        if (state.consecutiveSuccess >= stableThreshold && state.currentInterval < maxIntervalMs) {
            state.currentInterval = Math.min(state.currentInterval * 2, maxIntervalMs);
            state.consecutiveSuccess = 0;
            log.info("Heartbeat interval increased for user {}: {}ms (avgRTT={}ms)",
                    userId, state.currentInterval, String.format("%.1f", avgResponseTime));
        }

        totalHeartbeatsReceived.incrementAndGet();
        nettyChannelManager.recordHeartbeatReceived(userId);
        log.trace("Received heartbeat response from user {}, RTT={}ms", userId, roundTripTime);
    }

    /**
     * 处理心跳失败
     */
    public void handleHeartbeatFailure(String userId) {
        HeartbeatState state = heartbeatStates.get(userId);
        if (state == null) {
            return;
        }

        state.pending = false;
        state.consecutiveFailure++;
        state.consecutiveSuccess = 0;

        // 网络不稳定，缩短心跳间隔
        if (state.consecutiveFailure >= unstableThreshold && state.currentInterval > minIntervalMs) {
            state.currentInterval = Math.max(state.currentInterval / 2, minIntervalMs);
            log.warn("Heartbeat interval decreased for user {}: {}ms (consecutiveFailure={})",
                    userId, state.currentInterval, state.consecutiveFailure);
        }
    }

    /**
     * 检查心跳超时
     */
    @Scheduled(fixedRate = 5000)
    public void checkHeartbeatTimeout() {
        long now = System.currentTimeMillis();

        heartbeatStates.forEach((userId, state) -> {
            if (state.pending && now - state.lastHeartbeatSent > timeoutMs) {
                log.warn("Heartbeat timeout for user {}, lastSent={}, timeout={}ms",
                        userId, state.lastHeartbeatSent, timeoutMs);
                handleHeartbeatFailure(userId);
                totalHeartbeatsTimeout.incrementAndGet();
            }
        });
    }

    /**
     * 获取用户的心跳状态
     */
    public HeartbeatState getHeartbeatState(String userId) {
        return heartbeatStates.get(userId);
    }

    /**
     * 移除用户心跳状态
     */
    public void removeUser(String userId) {
        heartbeatStates.remove(userId);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalHeartbeatsSent", totalHeartbeatsSent.get());
        stats.put("totalHeartbeatsReceived", totalHeartbeatsReceived.get());
        stats.put("totalHeartbeatsTimeout", totalHeartbeatsTimeout.get());
        stats.put("activeUsers", heartbeatStates.size());
        stats.put("successRate", totalHeartbeatsSent.get() > 0
                ? String.format("%.2f%%", 100.0 * totalHeartbeatsReceived.get() / totalHeartbeatsSent.get())
                : "N/A");
        return stats;
    }

    /**
     * 获取当前配置
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new ConcurrentHashMap<>();
        config.put("defaultIntervalMs", defaultIntervalMs);
        config.put("minIntervalMs", minIntervalMs);
        config.put("maxIntervalMs", maxIntervalMs);
        config.put("stableThreshold", stableThreshold);
        config.put("unstableThreshold", unstableThreshold);
        config.put("timeoutMs", timeoutMs);
        return config;
    }

    @PreDestroy
    public void shutdown() {
        log.info("自适应心跳管理器关闭...");
        heartbeatStates.clear();
    }

    /**
     * 心跳状态
     */
    public static class HeartbeatState {
        /**
         * 当前心跳间隔（毫秒）
         */
        volatile long currentInterval;

        /**
         * 上次发送心跳时间
         */
        long lastHeartbeatSent;

        /**
         * 是否等待响应
         */
        volatile boolean pending;

        /**
         * 连续成功次数
         */
        int consecutiveSuccess;

        /**
         * 连续失败次数
         */
        int consecutiveFailure;

        /**
         * 最近响应时间记录
         */
        java.util.Queue<Long> responseTimes = new java.util.concurrent.ConcurrentLinkedDeque<>();

        public HeartbeatState(long interval) {
            this.currentInterval = interval;
        }
    }
}
