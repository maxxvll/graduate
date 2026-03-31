package com.maxxvll.netty;

import com.maxxvll.component.NettyChannelManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心跳监控定时任务
 * 定期检查心跳异常并记录日志
 */
@Component
@Slf4j
public class HeartbeatMonitor {

    /**
     * 心跳响应时间告警阈值（毫秒）
     * 超过此值认为是异常
     */
    @Value("${ws.heartbeat.alert-threshold-ms:5000}")
    private long alertThresholdMs;

    /**
     * 连续异常次数阈值
     * 超过此值触发告警
     */
    @Value("${ws.heartbeat.consecutive-alert-threshold:3}")
    private int consecutiveAlertThreshold;

    @Resource
    private NettyManagerHandle nettyManagerHandle;

    @Resource
    private NettyChannelManager nettyChannelManager;

    /**
     * 记录每个用户的连续异常次数
     */
    private final java.util.Map<String, Integer> consecutiveAnomalyCount = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 每60秒检查一次心跳异常
     */
    @Scheduled(fixedRate = 60000)
    public void checkHeartbeatAnomalies() {
        try {
            nettyManagerHandle.checkHeartbeatAnomalies();
        } catch (Exception e) {
            log.error("心跳异常检查失败", e);
        }
    }

    /**
     * 每30秒检查一次心跳响应时间
     */
    @Scheduled(fixedRate = 30000)
    public void checkHeartbeatResponseTime() {
        try {
            checkSlowHeartbeats();
        } catch (Exception e) {
            log.error("心跳响应时间检查失败", e);
        }
    }

    /**
     * 检查响应时间过慢的连接
     */
    private void checkSlowHeartbeats() {
        var heartbeatStats = nettyChannelManager.getHeartbeatStats();
        Double avgResponseTime = heartbeatStats.get("avgResponseTime") instanceof Number number
                ? number.doubleValue()
                : null;

        if (avgResponseTime == null || avgResponseTime <= 0) {
            return;
        }

        // 计算告警阈值（平均值的2倍或配置值的较大者）
        double threshold = Math.max(avgResponseTime * 2, alertThresholdMs);

        nettyChannelManager.getOnlineUsersSnapshot().forEach(userId -> {
            Long responseTime = nettyChannelManager.getHeartbeatResponseTime(userId);
            if (responseTime != null && responseTime > threshold) {
                int count = consecutiveAnomalyCount.getOrDefault(userId, 0) + 1;
                consecutiveAnomalyCount.put(userId, count);

                if (count >= consecutiveAlertThreshold) {
                    log.warn("检测到心跳响应异常: userId={}, 响应时间={}ms, 平均响应时间={}ms, 阈值={}ms, 连续异常次数={}",
                            userId, responseTime, avgResponseTime, threshold, count);
                } else {
                    log.debug("检测到心跳响应较慢: userId={}, 响应时间={}ms, 连续异常次数={}",
                            userId, responseTime, count);
                }
            } else {
                // 正常响应，清零连续异常计数
                consecutiveAnomalyCount.remove(userId);
            }
        });

        // 清理不在线用户的记录
        var onlineUsers = nettyChannelManager.getOnlineUsersSnapshot();
        consecutiveAnomalyCount.keySet().removeIf(userId -> !onlineUsers.contains(userId));
    }

    /**
     * 获取当前心跳统计摘要
     */
    public java.util.Map<String, Object> getHeartbeatSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        var stats = nettyChannelManager.getHeartbeatStats();
        summary.putAll(stats);
        summary.put("alertThresholdMs", alertThresholdMs);
        summary.put("usersWithAnomalies", consecutiveAnomalyCount.size());
        summary.put("anomalyUsers", consecutiveAnomalyCount);
        return summary;
    }

    /**
     * 获取连续异常的用户列表
     */
    public java.util.Map<String, Integer> getUsersWithConsecutiveAnomalies() {
        return new java.util.HashMap<>(consecutiveAnomalyCount);
    }

    /**
     * 清除指定用户的异常计数
     */
    public void clearAnomalyCount(String userId) {
        consecutiveAnomalyCount.remove(userId);
    }
}
