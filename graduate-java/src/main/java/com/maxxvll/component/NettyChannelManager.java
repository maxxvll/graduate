package com.maxxvll.component;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import com.maxxvll.netty.WebSocketConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class NettyChannelManager {

    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(5);
    private static final String UNKNOWN_DEVICE_TYPE = "unknown";

    @Resource
    private RedissonClient redissonClient;

    // 每用户保留多个 channel
    private final Map<String, CopyOnWriteArrayList<Channel>> channelMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> deviceTypeCounts = new ConcurrentHashMap<>();
    private final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final AtomicLong totalConnections = new AtomicLong(0);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong heartbeatsSent = new AtomicLong(0);
    private final AtomicLong heartbeatsReceived = new AtomicLong(0);
    private final Map<String, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    private final Map<String, Long> heartbeatResponseTimes = new ConcurrentHashMap<>();

    public void bindChannel(String userId, Channel channel, String deviceType) {
        if (userId == null || channel == null) {
            return;
        }

        String normalizedDeviceType = normalizeDeviceType(deviceType);

        // 获取用户的 channel 列表，不存在则创建
        CopyOnWriteArrayList<Channel> channelList = channelMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // 如果该 channel 已在列表中则跳过
        if (channelList.contains(channel)) {
            log.debug("Channel already bound, userId={}, deviceType={}", userId, normalizedDeviceType);
            return;
        }

        // 添加到列表 + channelGroup
        channelList.add(channel);
        channelGroup.add(channel);

        // 从 channel attribute 读取设备类型
        String channelDeviceType = channel.attr(WebSocketConstants.DEVICE_TYPE_KEY).get();
        if (channelDeviceType == null) {
            channelDeviceType = normalizedDeviceType;
        } else {
            channelDeviceType = normalizeDeviceType(channelDeviceType);
        }

        incrementDeviceTypeCount(channelDeviceType);
        totalConnections.incrementAndGet();
        syncOnlineStatus(userId, true);

        log.debug("Bound channel, userId={}, deviceType={}, activeConnections={}",
                userId, channelDeviceType, getActiveConnections());
    }

    public void bindChannel(String userId, Channel channel) {
        bindChannel(userId, channel, UNKNOWN_DEVICE_TYPE);
    }

    /**
     * 向后兼容：返回用户第一个活跃 channel
     */
    public Channel getChannel(String userId) {
        CopyOnWriteArrayList<Channel> channelList = channelMap.get(userId);
        if (channelList == null || channelList.isEmpty()) {
            return null;
        }
        // 返回第一个活跃的 channel
        for (Channel channel : channelList) {
            if (channel.isActive()) {
                return channel;
            }
        }
        // 如果都 inactive，清理并返回 null
        cleanupInactiveChannels(userId, channelList);
        return null;
    }

    /**
     * 移除特定 channel（用于 channelInactive 精确移除）
     * @return 用户是否完全离线（所有 channel 都已断开）
     */
    public boolean removeChannel(String userId, Channel channel) {
        if (userId == null || channel == null) {
            return true;
        }

        CopyOnWriteArrayList<Channel> channelList = channelMap.get(userId);
        if (channelList == null || channelList.isEmpty()) {
            return true;
        }

        // 从列表中移除该 channel
        if (!channelList.remove(channel)) {
            // channel 不在列表中，可能已经被移除
            return channelList.isEmpty();
        }

        // 从 channelGroup 中移除
        channelGroup.remove(channel);

        // 更新设备类型计数
        String channelDeviceType = channel.attr(WebSocketConstants.DEVICE_TYPE_KEY).get();
        if (channelDeviceType != null) {
            decrementDeviceTypeCount(normalizeDeviceType(channelDeviceType));
        }

        // 如果列表为空，删除用户并标记离线
        if (channelList.isEmpty()) {
            channelMap.remove(userId);
            syncOnlineStatus(userId, false);
            log.debug("All channels removed, userId={}, activeConnections={}",
                    userId, getActiveConnections());
            return true; // 用户完全离线
        }

        // 列表非空，用户仍在线
        log.debug("Removed one channel, userId={}, remainingChannels={}, activeConnections={}",
                userId, channelList.size(), getActiveConnections());
        return false; // 用户仍在线
    }

    /**
     * 移除用户的所有 channel（用于强制踢人）
     */
    public void removeChannel(String userId) {
        if (userId == null) {
            return;
        }

        CopyOnWriteArrayList<Channel> channelList = channelMap.remove(userId);
        if (channelList == null || channelList.isEmpty()) {
            return;
        }

        // 关闭并移除所有 channel
        for (Channel channel : channelList) {
            channelGroup.remove(channel);
            String channelDeviceType = channel.attr(WebSocketConstants.DEVICE_TYPE_KEY).get();
            if (channelDeviceType != null) {
                decrementDeviceTypeCount(normalizeDeviceType(channelDeviceType));
            }
        }

        syncOnlineStatus(userId, false);
        log.debug("Removed all channels, userId={}, count={}, activeConnections={}",
                userId, channelList.size(), getActiveConnections());
    }

    public boolean sendMessageToUser(String userId, String message) {
        return sendSerializedMessageToUser(userId, message);
    }

    public boolean sendSerializedMessageToUser(String userId, String messageJson) {
        CopyOnWriteArrayList<Channel> channelList = channelMap.get(userId);
        if (channelList == null || channelList.isEmpty()) {
            return false;
        }

        boolean atLeastOneSuccess = false;
        boolean hasInactive = false;

        for (Channel channel : channelList) {
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(messageJson));
                messagesSent.incrementAndGet();
                atLeastOneSuccess = true;
            } else {
                hasInactive = true;
            }
        }

        // 清理不活跃的 channel
        if (hasInactive) {
            cleanupInactiveChannels(userId, channelList);
        }

        return atLeastOneSuccess;
    }

    /**
     * 清理用户的不活跃 channel
     */
    private void cleanupInactiveChannels(String userId, CopyOnWriteArrayList<Channel> channelList) {
        if (channelList == null || channelList.isEmpty()) {
            return;
        }

        boolean removed = false;
        for (Channel channel : new ArrayList<>(channelList)) {
            if (!channel.isActive()) {
                channelList.remove(channel);
                channelGroup.remove(channel);

                String channelDeviceType = channel.attr(WebSocketConstants.DEVICE_TYPE_KEY).get();
                if (channelDeviceType != null) {
                    decrementDeviceTypeCount(normalizeDeviceType(channelDeviceType));
                }
                removed = true;
            }
        }

        if (removed) {
            // 如果列表为空，删除用户并标记离线
            if (channelList.isEmpty()) {
                channelMap.remove(userId);
                syncOnlineStatus(userId, false);
                log.debug("All channels cleaned up, userId={}", userId);
            }
            log.debug("Cleaned up inactive channels, userId={}, remaining={}", userId, channelList.size());
        }
    }

    public void broadcast(String message) {
        channelGroup.writeAndFlush(new TextWebSocketFrame(message));
        messagesSent.addAndGet(channelGroup.size());
    }

    public Map<String, Object> getMetrics() {
        Map<String, Integer> connectionsPerDevice = new HashMap<>();
        deviceTypeCounts.forEach((deviceType, count) -> connectionsPerDevice.put(deviceType, count.get()));

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeConnections", getActiveConnections());
        metrics.put("totalConnections", totalConnections.get());
        metrics.put("messagesSent", messagesSent.get());
        metrics.put("messagesReceived", messagesReceived.get());
        metrics.put("connectionsPerDevice", connectionsPerDevice);
        return metrics;
    }

    public void recordMessageReceived() {
        messagesReceived.incrementAndGet();
    }

    public int getActiveConnections() {
        int count = 0;
        for (CopyOnWriteArrayList<Channel> channelList : channelMap.values()) {
            for (Channel channel : channelList) {
                if (channel.isActive()) {
                    count++;
                }
            }
        }
        return count;
    }

    public long getTotalConnections() {
        return totalConnections.get();
    }

    public Set<String> getOnlineUsers() {
        return new HashSet<>(channelMap.keySet());
    }

    public Set<String> getOnlineUsersSnapshot() {
        return new HashSet<>(channelMap.keySet());
    }

    public boolean isUserOnline(String userId) {
        CopyOnWriteArrayList<Channel> channelList = channelMap.get(userId);
        if (channelList == null || channelList.isEmpty()) {
            return false;
        }
        // 只要有一个活跃 channel 就认为在线
        for (Channel channel : channelList) {
            if (channel.isActive()) {
                return true;
            }
        }
        // 都 inactive，清理
        cleanupInactiveChannels(userId, channelList);
        return false;
    }

    public boolean disconnectUser(String userId) {
        CopyOnWriteArrayList<Channel> channelList = channelMap.get(userId);
        if (channelList == null || channelList.isEmpty()) {
            return false;
        }

        boolean disconnected = false;
        for (Channel channel : channelList) {
            if (channel.isActive()) {
                channel.close();
                disconnected = true;
            }
        }
        return disconnected;
    }

    public void recordHeartbeatSent(String userId) {
        heartbeatsSent.incrementAndGet();
        lastHeartbeatTime.put(userId, System.currentTimeMillis());
    }

    public void recordHeartbeatReceived(String userId) {
        heartbeatsReceived.incrementAndGet();
        Long lastSentTime = lastHeartbeatTime.get(userId);
        if (lastSentTime != null) {
            heartbeatResponseTimes.put(userId, System.currentTimeMillis() - lastSentTime);
        }
    }

    public Map<String, Object> getHeartbeatStats() {
        double avgResponseTime = heartbeatResponseTimes.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        long maxResponseTime = heartbeatResponseTimes.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);

        Map<String, Object> stats = new HashMap<>();
        stats.put("heartbeatsSent", heartbeatsSent.get());
        stats.put("heartbeatsReceived", heartbeatsReceived.get());
        stats.put("avgResponseTime", avgResponseTime);
        stats.put("maxResponseTime", maxResponseTime);
        return stats;
    }

    /**
     * 清理用户心跳数据（仅在用户完全离线时调用）
     */
    public void cleanupUserHeartbeatData(String userId) {
        // 只有用户完全离线时才清理心跳数据
        if (!isUserOnline(userId)) {
            lastHeartbeatTime.remove(userId);
            heartbeatResponseTimes.remove(userId);
        }
    }

    public Long getHeartbeatResponseTime(String userId) {
        return heartbeatResponseTimes.get(userId);
    }

    public Long getLastHeartbeatTime(String userId) {
        return lastHeartbeatTime.get(userId);
    }

    private void incrementDeviceTypeCount(String deviceType) {
        deviceTypeCounts.computeIfAbsent(deviceType, ignored -> new AtomicInteger(0)).incrementAndGet();
    }

    private void decrementDeviceTypeCount(String deviceType) {
        AtomicInteger count = deviceTypeCounts.get(deviceType);
        if (count == null) {
            return;
        }

        if (count.decrementAndGet() <= 0) {
            deviceTypeCounts.remove(deviceType);
        }
    }

    private String normalizeDeviceType(String deviceType) {
        return deviceType == null || deviceType.isBlank() ? UNKNOWN_DEVICE_TYPE : deviceType;
    }

    private void syncOnlineStatus(String userId, boolean online) {
        try {
            RBucket<Boolean> bucket = redissonClient.getBucket(ONLINE_KEY_PREFIX + userId);
            if (online) {
                bucket.set(Boolean.TRUE, ONLINE_TTL);
            } else {
                bucket.delete();
            }
        } catch (Exception e) {
            log.error("Sync online status failed, userId={}, online={}", userId, online, e);
        }
    }
}
