package com.maxxvll.component;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 优化的 Netty Channel 管理器
 *
 * 性能优化：
 * 1. 连接分区（16 个分区）减少锁竞争
 * 2. 定时清理过期心跳数据
 * 3. 优化的内存管理
 */
@Component
@Slf4j
public class OptimizedNettyChannelManager {

    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5);
    private static final String UNKNOWN_DEVICE_TYPE = "unknown";

    /**
     * 连接分区数（16 个分区，减少锁竞争）
     */
    private static final int PARTITION_COUNT = 16;

    /**
     * 单用户最大连接数（防止恶意占用连接资源）
     */
    private static final int MAX_CONNECTIONS_PER_USER = 5;

    @Value("${ws.max-connections-per-user:5}")
    private int maxConnectionsPerUser;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 用户多端连接映射：userId -> List<DeviceConnection>
     * 支持同一用户多个设备同时在线
     */
    private final Map<String, List<DeviceConnection>> userMultiConnections = new ConcurrentHashMap<>();

    /**
     * Channel ID 到 userId 的反向映射（用于快速查找）
     */
    private final Map<String, String> channelIdToUserId = new ConcurrentHashMap<>();

    /**
     * 分区存储 Channel（按 userId hash 分区）
     */
    private final List<Map<String, Channel>> channelPartitions;

    /**
     * 分区存储设备类型
     */
    private final List<Map<String, String>> deviceTypePartitions;

    /**
     * 存储每个设备类型的连接数
     */
    private final Map<String, AtomicInteger> deviceTypeCounts = new ConcurrentHashMap<>();

    /**
     * 存储所有活跃的Channel
     */
    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 统计指标
     */
    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);

    /**
     * 心跳统计（分区存储）
     */
    private final List<Map<String, Long>> lastHeartbeatTimePartitions;
    private final List<Map<String, Long>> heartbeatResponseTimePartitions;

    /**
     * 心跳统计计数器
     */
    private final AtomicLong heartbeatsSent = new AtomicLong(0);
    private final AtomicLong heartbeatsReceived = new AtomicLong(0);

    public OptimizedNettyChannelManager() {
        // 初始化分区
        channelPartitions = new ArrayList<>(PARTITION_COUNT);
        deviceTypePartitions = new ArrayList<>(PARTITION_COUNT);
        lastHeartbeatTimePartitions = new ArrayList<>(PARTITION_COUNT);
        heartbeatResponseTimePartitions = new ArrayList<>(PARTITION_COUNT);

        for (int i = 0; i < PARTITION_COUNT; i++) {
            channelPartitions.add(new ConcurrentHashMap<>());
            deviceTypePartitions.add(new ConcurrentHashMap<>());
            lastHeartbeatTimePartitions.add(new ConcurrentHashMap<>());
            heartbeatResponseTimePartitions.add(new ConcurrentHashMap<>());
        }

        log.info("优化版 NettyChannelManager 初始化完成，分区数={}", PARTITION_COUNT);
    }

    /**
     * 设备连接信息
     */
    public record DeviceConnection(
            String channelId,
            String userId,
            String deviceType,
            String remoteAddress,
            long connectTime
    ) {}

    /**
     * 检查用户连接数是否超过限制
     */
    public boolean isUserConnectionLimitExceeded(String userId) {
        List<DeviceConnection> connections = userMultiConnections.get(userId);
        if (connections == null) {
            return false;
        }
        // 移除已断开的连接
        connections.removeIf(conn -> {
            int partitionIndex = getPartitionIndex(conn.userId());
            Map<String, Channel> partition = channelPartitions.get(partitionIndex);
            Channel channel = partition.get(conn.userId());
            return channel == null || !channel.id().asLongText().equals(conn.channelId());
        });
        return connections.size() >= maxConnectionsPerUser;
    }

    /**
     * 获取用户所有活跃连接的设备类型列表
     */
    public List<String> getUserDeviceTypes(String userId) {
        List<DeviceConnection> connections = userMultiConnections.get(userId);
        if (connections == null) {
            return List.of();
        }
        return connections.stream()
                .map(DeviceConnection::deviceType)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的活跃连接数
     */
    public int getUserConnectionCount(String userId) {
        List<DeviceConnection> connections = userMultiConnections.get(userId);
        if (connections == null) {
            return 0;
        }
        // 清理已断开的连接
        connections.removeIf(conn -> {
            int partitionIndex = getPartitionIndex(conn.userId());
            Map<String, Channel> partition = channelPartitions.get(partitionIndex);
            Channel channel = partition.get(conn.userId());
            return channel == null || !channel.id().asLongText().equals(conn.channelId());
        });
        return connections.size();
    }

    /**
     * 获取用户所在的分区索引
     */
    private int getPartitionIndex(String userId) {
        return Math.abs(userId.hashCode()) % PARTITION_COUNT;
    }

    /**
     * 标准化设备类型
     */
    private String normalizeDeviceType(String deviceType) {
        return deviceType == null || deviceType.isBlank() ? UNKNOWN_DEVICE_TYPE : deviceType;
    }

    /**
     * 获取远程地址
     */
    private String getRemoteAddress(Channel channel) {
        try {
            if (channel.remoteAddress() instanceof InetSocketAddress addr) {
                return addr.getAddress().getHostAddress() + ":" + addr.getPort();
            }
        } catch (Exception e) {
            log.debug("获取远程地址失败", e);
        }
        return "unknown";
    }

    /**
     * 同步在线状态到 Redis
     */
    private void syncOnlineStatus(String userId, boolean online) {
        try {
            RBucket<Boolean> bucket = redissonClient.getBucket(ONLINE_KEY_PREFIX + userId);
            if (online) {
                bucket.set(Boolean.TRUE, ONLINE_TTL);
            } else {
                bucket.delete();
            }
        } catch (Exception e) {
            log.error("同步在线状态失败，userId={}, online={}", userId, online, e);
        }
    }

    /**
     * 绑定Channel（带设备类型）
     */
    public void bindChannel(String userId, Channel channel, String deviceType) {
        if (userId != null && channel != null) {
            int partitionIndex = getPartitionIndex(userId);
            String normalizedDeviceType = normalizeDeviceType(deviceType);
            String channelId = channel.id().asLongText();
            String remoteAddress = getRemoteAddress(channel);

            // 检查用户连接数限制
            if (isUserConnectionLimitExceeded(userId)) {
                log.warn("用户连接数超过限制，拒绝连接，userId={}, deviceType={}, maxConnections={}",
                        userId, normalizedDeviceType, maxConnectionsPerUser);
                channel.close();
                return;
            }

            // 存储到对应分区
            channelPartitions.get(partitionIndex).put(userId, channel);
            channelGroup.add(channel);

            // 记录设备类型
            deviceTypePartitions.get(partitionIndex).put(userId, normalizedDeviceType);
            deviceTypeCounts.computeIfAbsent(normalizedDeviceType, k -> new AtomicInteger(0)).incrementAndGet();

            // 更新 Channel ID 映射
            channelIdToUserId.put(channelId, userId);

            // 更新多端连接映射
            userMultiConnections.computeIfAbsent(userId, k -> new ArrayList<>())
                    .add(new DeviceConnection(channelId, userId, normalizedDeviceType, remoteAddress, System.currentTimeMillis()));

            // 同步在线状态到 Redis
            syncOnlineStatus(userId, true);

            // 更新统计
            totalConnections.incrementAndGet();
            log.info("Channel绑定成功：userId={}, deviceType={}, channelId={}, remoteAddress={}, partition={}, 用户连接数={}",
                    userId, normalizedDeviceType, channelId, remoteAddress, partitionIndex, getUserConnectionCount(userId));
        }
    }

    /**
     * 兼容旧版本的方法（不带设备类型）
     */
    public void bindChannel(String userId, Channel channel) {
        bindChannel(userId, channel, "unknown");
    }

    /**
     * 获取Channel
     */
    public Channel getChannel(String userId) {
        if (userId == null) {
            return null;
        }
        int partitionIndex = getPartitionIndex(userId);
        return channelPartitions.get(partitionIndex).get(userId);
    }

    /**
     * 移除Channel（修复内存泄漏）
     */
    public void removeChannel(String userId) {
        if (userId != null) {
            int partitionIndex = getPartitionIndex(userId);

            Channel channel = channelPartitions.get(partitionIndex).remove(userId);
            String channelId = channel != null ? channel.id().asLongText() : null;

            // 清理设备类型映射
            String deviceType = deviceTypePartitions.get(partitionIndex).remove(userId);
            if (deviceType != null) {
                AtomicInteger count = deviceTypeCounts.get(deviceType);
                if (count != null && count.decrementAndGet() <= 0) {
                    deviceTypeCounts.remove(deviceType);
                }
            }

            // 清理心跳数据（修复内存泄漏）
            lastHeartbeatTimePartitions.get(partitionIndex).remove(userId);
            heartbeatResponseTimePartitions.get(partitionIndex).remove(userId);

            // 清理 Channel ID 映射
            if (channelId != null) {
                channelIdToUserId.remove(channelId);
            }

            // 清理多端连接映射
            if (channelId != null) {
                List<DeviceConnection> connections = userMultiConnections.get(userId);
                if (connections != null) {
                    connections.removeIf(conn -> conn.channelId().equals(channelId));
                    if (connections.isEmpty()) {
                        userMultiConnections.remove(userId);
                        // 只有当用户所有连接都断开时才更新 Redis 在线状态
                        syncOnlineStatus(userId, false);
                    }
                }
            }

            // 从ChannelGroup中移除
            if (channel != null) {
                channelGroup.remove(channel);
            }

            log.info("Channel移除成功：userId={}, deviceType={}, channelId={}, partition={}, 用户剩余连接数={}",
                    userId, deviceType, channelId, partitionIndex, getUserConnectionCount(userId));
        }
    }

    /**
     * 根据 Channel ID 移除连接
     */
    public void removeChannelByChannelId(String channelId) {
        String userId = channelIdToUserId.get(channelId);
        if (userId != null) {
            removeChannel(userId);
        }
    }

    /**
     * 发送消息给指定用户
     */
    public boolean sendMessageToUser(String userId, String message) {
        Channel channel = getChannel(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
            messagesSent.incrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * 广播消息给所有用户
     */
    public void broadcast(String message) {
        channelGroup.writeAndFlush(message);
        messagesSent.addAndGet(channelGroup.size());
    }

    /**
     * 获取当前连接统计
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("activeConnections", getActiveConnections());
        metrics.put("totalConnections", totalConnections.get());
        metrics.put("messagesSent", messagesSent.get());
        metrics.put("messagesReceived", messagesReceived.get());
        metrics.put("connectionsPerDevice", new ConcurrentHashMap<>(deviceTypeCounts));
        metrics.put("partitionCount", PARTITION_COUNT);
        metrics.put("multiConnectionUsers", userMultiConnections.size());
        metrics.put("totalMultiConnections", userMultiConnections.values().stream().mapToInt(List::size).sum());
        metrics.put("maxConnectionsPerUser", maxConnectionsPerUser);
        return metrics;
    }

    /**
     * 记录接收到的消息
     */
    public void recordMessageReceived() {
        messagesReceived.incrementAndGet();
    }

    /**
     * 获取活跃连接数
     */
    public int getActiveConnections() {
        return channelPartitions.stream()
                .mapToInt(Map::size)
                .sum();
    }

    /**
     * 获取总连接数
     */
    public long getTotalConnections() {
        return totalConnections.get();
    }

    /**
     * 获取所有在线用户ID
     */
    public Set<String> getOnlineUsers() {
        return channelPartitions.stream()
                .flatMap(map -> map.keySet().stream())
                .collect(Collectors.toSet());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        Channel channel = getChannel(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 断开指定用户的连接
     */
    public boolean disconnectUser(String userId) {
        Channel channel = getChannel(userId);
        if (channel != null && channel.isActive()) {
            channel.close();
            return true;
        }
        return false;
    }

    /**
     * 记录心跳发送
     */
    public void recordHeartbeatSent(String userId) {
        heartbeatsSent.incrementAndGet();
        int partitionIndex = getPartitionIndex(userId);
        lastHeartbeatTimePartitions.get(partitionIndex).put(userId, System.currentTimeMillis());
    }

    /**
     * 记录心跳接收并计算响应时间
     */
    public void recordHeartbeatReceived(String userId) {
        heartbeatsReceived.incrementAndGet();
        int partitionIndex = getPartitionIndex(userId);
        Long lastSentTime = lastHeartbeatTimePartitions.get(partitionIndex).get(userId);
        if (lastSentTime != null) {
            long responseTime = System.currentTimeMillis() - lastSentTime;
            heartbeatResponseTimePartitions.get(partitionIndex).put(userId, responseTime);
        }
    }

    /**
     * 获取心跳统计信息
     */
    public Map<String, Object> getHeartbeatStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("heartbeatsSent", heartbeatsSent.get());
        stats.put("heartbeatsReceived", heartbeatsReceived.get());

        // 收集所有响应时间
        List<Long> allResponseTimes = heartbeatResponseTimePartitions.stream()
                .flatMap(map -> map.values().stream())
                .collect(Collectors.toList());

        // 计算平均响应时间
        double avgResponseTime = allResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        stats.put("avgResponseTime", avgResponseTime);

        // 计算最大响应时间
        long maxResponseTime = allResponseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        stats.put("maxResponseTime", maxResponseTime);

        return stats;
    }

    /**
     * 清理用户心跳数据
     */
    public void cleanupUserHeartbeatData(String userId) {
        int partitionIndex = getPartitionIndex(userId);
        lastHeartbeatTimePartitions.get(partitionIndex).remove(userId);
        heartbeatResponseTimePartitions.get(partitionIndex).remove(userId);
    }

    /**
     * 获取用户最后心跳响应时间
     */
    public Long getHeartbeatResponseTime(String userId) {
        int partitionIndex = getPartitionIndex(userId);
        return heartbeatResponseTimePartitions.get(partitionIndex).get(userId);
    }

    /**
     * 获取用户最后心跳时间戳
     */
    public Long getLastHeartbeatTime(String userId) {
        int partitionIndex = getPartitionIndex(userId);
        return lastHeartbeatTimePartitions.get(partitionIndex).get(userId);
    }

    /**
     * 定时清理过期心跳数据（兜底机制，防止内存泄漏）
     * 每 5 分钟执行一次，清理超过 10 分钟未活动的心跳数据
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupStaleHeartbeatData() {
        long threshold = System.currentTimeMillis() - 600000; // 10分钟前
        int totalCleaned = 0;

        for (int i = 0; i < PARTITION_COUNT; i++) {
            final int partitionIndex = i;
            Map<String, Long> lastHeartbeatMap = lastHeartbeatTimePartitions.get(i);
            Map<String, Long> responseTimeMap = heartbeatResponseTimePartitions.get(i);

            // 清理过期数据
            int beforeSize = lastHeartbeatMap.size();
            lastHeartbeatMap.entrySet().removeIf(entry -> {
                boolean stale = entry.getValue() < threshold;
                if (stale) {
                    String userId = entry.getKey();
                    responseTimeMap.remove(userId);
                    log.debug("清理过期心跳数据：userId={}, partition={}", userId, partitionIndex);
                }
                return stale;
            });
            int cleaned = beforeSize - lastHeartbeatMap.size();

            totalCleaned += cleaned;
        }

        if (totalCleaned > 0) {
            log.info("定时清理过期心跳数据完成：清理数量={}, 剩余心跳记录={}",
                    totalCleaned,
                    lastHeartbeatTimePartitions.stream()
                            .mapToInt(Map::size)
                            .sum());
        }
    }

    /**
     * 获取各分区的连接数分布（用于监控）
     */
    public Map<String, Object> getPartitionDistribution() {
        Map<String, Object> distribution = new ConcurrentHashMap<>();
        List<Integer> partitionSizes = new ArrayList<>();

        for (int i = 0; i < PARTITION_COUNT; i++) {
            int size = channelPartitions.get(i).size();
            partitionSizes.add(size);
            distribution.put("partition_" + i, size);
        }

        // 计算标准差（检测分区是否均匀）
        double avg = partitionSizes.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double variance = partitionSizes.stream()
                .mapToDouble(size -> Math.pow(size - avg, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        distribution.put("average", avg);
        distribution.put("stdDev", stdDev);
        distribution.put("balance", stdDev < avg * 0.3 ? "GOOD" : "POOR");

        return distribution;
    }
}
