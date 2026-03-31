package com.maxxvll.controller;

import com.maxxvll.common.Result;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.netty.ConnectionMetrics;
import com.maxxvll.netty.WebSocketAuthInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ws-admin")
@Slf4j
public class WebSocketMonitorController {

    private static final long STARTUP_TIME = Instant.now().getEpochSecond();

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private WebSocketAuthInterceptor authInterceptor;

    @GetMapping("/metrics")
    public Result<ConnectionMetrics> getMetrics() {
        try {
            Map<String, Object> rawMetrics = nettyChannelManager.getMetrics();
            Map<String, Object> authStats = authInterceptor.getConnectionStats();
            Map<String, Object> heartbeatStats = nettyChannelManager.getHeartbeatStats();

            long uptimeSeconds = Instant.now().getEpochSecond() - STARTUP_TIME;

            ConnectionMetrics metrics = ConnectionMetrics.builder()
                    .activeConnections(getInt(rawMetrics, "activeConnections"))
                    .totalConnections(getLong(rawMetrics, "totalConnections"))
                    .messagesSent(getLong(rawMetrics, "messagesSent"))
                    .messagesReceived(getLong(rawMetrics, "messagesReceived"))
                    .avgLatency(getDouble(heartbeatStats, "avgResponseTime"))
                    .connectionsPerDevice(getStringIntMap(rawMetrics, "connectionsPerDevice"))
                    .messagesPerSecond(calculateMessagesPerSecond(rawMetrics))
                    .connectionSuccessRate(calculateConnectionSuccessRate(rawMetrics))
                    .uptimeSeconds(uptimeSeconds)
                    .avgConnectionsPerIp(calculateAvgConnectionsPerIp(authStats))
                    .avgConnectionsPerUser(calculateAvgConnectionsPerUser(authStats))
                    .onlineUserIds(Set.copyOf(nettyChannelManager.getOnlineUsers()))
                    .serverTimestamp(System.currentTimeMillis())
                    .build();

            log.debug("Return websocket metrics: {}", metrics);
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("Get websocket metrics failed", e);
            return Result.fail("Get websocket metrics failed: " + e.getMessage());
        }
    }

    @PostMapping("/disconnect/{userId}")
    public Result<String> disconnectUser(@PathVariable String userId) {
        try {
            boolean disconnected = nettyChannelManager.disconnectUser(userId);
            if (!disconnected) {
                log.warn("User is offline or connection does not exist, userId={}", userId);
                return Result.fail("User is offline or connection does not exist");
            }

            log.info("Disconnect user success, userId={}", userId);
            return Result.success("Disconnected user " + userId);
        } catch (Exception e) {
            log.error("Disconnect user failed, userId={}", userId, e);
            return Result.fail("Disconnect user failed: " + e.getMessage());
        }
    }

    @PostMapping("/broadcast")
    public Result<String> broadcast(@RequestBody BroadcastMessage message) {
        try {
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return Result.fail("Broadcast message content cannot be empty");
            }

            nettyChannelManager.broadcast(message.getContent());
            int activeConnections = nettyChannelManager.getActiveConnections();
            log.info("Broadcast success, receivers={}, content={}", activeConnections, message.getContent());
            return Result.success("Broadcast sent to " + activeConnections + " users");
        } catch (Exception e) {
            log.error("Broadcast failed", e);
            return Result.fail("Broadcast failed: " + e.getMessage());
        }
    }

    @PostMapping("/send/{userId}")
    public Result<String> sendToUser(@PathVariable String userId,
                                     @RequestBody Map<String, String> message) {
        try {
            String content = message.get("content");
            if (content == null || content.trim().isEmpty()) {
                return Result.fail("Message content cannot be empty");
            }

            boolean sent = nettyChannelManager.sendMessageToUser(userId, content);
            if (!sent) {
                log.warn("User is offline, userId={}", userId);
                return Result.fail("User is offline");
            }

            log.info("Send admin message success, userId={}", userId);
            return Result.success("Message sent");
        } catch (Exception e) {
            log.error("Send admin message failed, userId={}", userId, e);
            return Result.fail("Send admin message failed: " + e.getMessage());
        }
    }

    @GetMapping("/online-users")
    public Result<Set<String>> getOnlineUsers() {
        try {
            Set<String> onlineUsers = Set.copyOf(nettyChannelManager.getOnlineUsers());
            log.debug("Return online users, count={}", onlineUsers.size());
            return Result.success(onlineUsers);
        } catch (Exception e) {
            log.error("Get online users failed", e);
            return Result.fail("Get online users failed: " + e.getMessage());
        }
    }

    @GetMapping("/online/{userId}")
    public Result<Boolean> isUserOnline(@PathVariable String userId) {
        try {
            return Result.success(nettyChannelManager.isUserOnline(userId));
        } catch (Exception e) {
            log.error("Check user online status failed, userId={}", userId, e);
            return Result.fail("Check user online status failed: " + e.getMessage());
        }
    }

    public static class BroadcastMessage {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private double calculateMessagesPerSecond(Map<String, Object> rawMetrics) {
        long messagesSent = getLong(rawMetrics, "messagesSent");
        long uptimeSeconds = Instant.now().getEpochSecond() - STARTUP_TIME;
        if (uptimeSeconds <= 0) {
            return 0;
        }
        return (double) messagesSent / uptimeSeconds;
    }

    private double calculateConnectionSuccessRate(Map<String, Object> rawMetrics) {
        long totalConnections = getLong(rawMetrics, "totalConnections");
        int activeConnections = getInt(rawMetrics, "activeConnections");
        if (totalConnections <= 0) {
            return 100.0;
        }
        return (double) activeConnections / totalConnections * 100;
    }

    private double calculateAvgConnectionsPerIp(Map<String, Object> authStats) {
        return averageValue(getStringIntMap(authStats, "ipConnectionCounts"));
    }

    private double calculateAvgConnectionsPerUser(Map<String, Object> authStats) {
        return averageValue(getStringIntMap(authStats, "userConnectionCounts"));
    }

    private double averageValue(Map<String, Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        return (double) total / values.size();
    }

    private int getInt(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long getLong(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double getDouble(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private Map<String, Integer> getStringIntMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        rawMap.forEach((rawKey, rawValue) -> {
            if (rawKey != null && rawValue instanceof Number number) {
                result.put(rawKey.toString(), number.intValue());
            }
        });
        return result;
    }
}
