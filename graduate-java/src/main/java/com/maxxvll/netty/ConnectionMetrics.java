package com.maxxvll.netty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebSocket 连接指标数据类
 * 用于监控和统计 WebSocket 连接状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionMetrics {

    /**
     * 当前活跃连接数
     */
    private int activeConnections;

    /**
     * 历史总连接数（自服务器启动以来的累计连接数）
     */
    private long totalConnections;

    /**
     * 发送消息总数
     */
    private long messagesSent;

    /**
     * 接收消息总数
     */
    private long messagesReceived;

    /**
     * 平均消息延迟（毫秒）
     */
    private double avgLatency;

    /**
     * 各设备类型的连接数
     * 例如：{"mobile": 150, "pc": 80, "web": 120, "unknown": 5}
     */
    private Map<String, Integer> connectionsPerDevice;

    /**
     * 每秒消息数（最近1分钟）
     */
    private double messagesPerSecond;

    /**
     * 连接成功率（百分比）
     */
    private double connectionSuccessRate;

    /**
     * 系统运行时间（秒）
     */
    private long uptimeSeconds;

    /**
     * 每个IP的平均连接数
     */
    private double avgConnectionsPerIp;

    /**
     * 每个用户的平均连接数
     */
    private double avgConnectionsPerUser;

    /**
     * 在线用户列表（仅返回用户ID，不包含敏感信息）
     */
    private java.util.Set<String> onlineUserIds;

    /**
     * 服务器时间戳
     */
    private long serverTimestamp;
}
