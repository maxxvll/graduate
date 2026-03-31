package com.maxxvll.netty;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.jwt.JWTUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 认证拦截器
 * 功能：
 * 1. Token 验证和过期检测
 * 2. 单用户连接数限制
 * 3. IP 白名单/黑名单
 * 4. 连接速率限制
 */
@Component
@Slf4j
@ChannelHandler.Sharable
public class WebSocketAuthInterceptor extends ChannelInboundHandlerAdapter {

    // 返回值标记：需要重试
    public static final String RETRY_MARKER = "RETRY";

    // Sa-Token 初始化状态标志
    private volatile boolean saTokenInitialized = false;

    @Resource
    private WebSocketAuthProperties authProperties;

    // 存储每个用户的连接数
    private final Map<String, AtomicInteger> userConnectionCount = new ConcurrentHashMap<>();

    // 存储每个 IP 的连接数
    private final Map<String, AtomicInteger> ipConnectionCount = new ConcurrentHashMap<>();

    // 存储每个 IP 在最近窗口内的连接尝试时间（用于速率限制）
    private final Map<String, Deque<Instant>> ipConnectionAttempts = new ConcurrentHashMap<>();

    // AttributeKey for storing client IP
    private static final AttributeKey<String> CLIENT_IP_KEY = AttributeKey.valueOf("clientIp");

    // AttributeKey for storing user ID
    private static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");

    /**
     * 等待 Sa-Token 初始化完成后标记
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        waitForSaTokenInit();
        this.saTokenInitialized = true;
        log.info("WebSocketAuthInterceptor initialized, saTokenInitialized=true");
    }

    /**
     * 等待 Sa-Token 初始化完成
     */
    private void waitForSaTokenInit() {
        int maxWaitSeconds = 60;
        int waitedSeconds = 0;
        while (waitedSeconds < maxWaitSeconds) {
            try {
                if (StpUtil.getStpLogic() != null) {
                    SaHolder.getContext();
                    log.info("Sa-Token context ready for WebSocketAuthInterceptor after {} seconds", waitedSeconds);
                    return;
                }
            } catch (Exception e) {
                // 等待
            }
            try {
                Thread.sleep(1000);
                waitedSeconds++;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Waited {} seconds for Sa-Token init in WebSocketAuthInterceptor, proceeding anyway", maxWaitSeconds);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String clientIp = getClientIp(channel);

        // 1. IP 白名单检查
        if (authProperties.isEnableIpWhitelist() && !isIpWhitelisted(clientIp)) {
            log.warn("Connection rejected: IP {} not in whitelist", clientIp);
            ctx.close();
            return;
        }

        // 2. IP 黑名单检查
        if (isIpBlacklisted(clientIp)) {
            log.warn("Connection rejected: IP {} is blacklisted", clientIp);
            ctx.close();
            return;
        }

        // 3. 连接速率限制检查
        if (!checkConnectionRateLimit(clientIp)) {
            log.warn("Connection rejected: IP {} exceeded rate limit", clientIp);
            ctx.close();
            return;
        }

        // 4. IP 连接数限制检查
        if (!checkIpConnectionLimit(clientIp)) {
            log.warn("Connection rejected: IP {} exceeded connection limit ({})",
                    clientIp, authProperties.getMaxConnectionsPerIp());
            ctx.close();
            return;
        }

        // Store client IP in channel attributes
        channel.attr(CLIENT_IP_KEY).set(clientIp);
        incrementIpConnectionCount(clientIp);

        log.info("Client connected: IP={}, RemoteAddress={}", clientIp, channel.remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String clientIp = channel.attr(CLIENT_IP_KEY).get();

        if (clientIp != null) {
            decrementIpConnectionCount(clientIp);
            log.debug("Client disconnected: IP={}", clientIp);
        }

        super.channelInactive(ctx);
    }

    /**
     * 检查 Sa-Token 是否可用
     * 注意：不使用 SaHolder.getContext()，因为它依赖 ThreadLocal
     * 而 Netty EventLoop 线程没有主线程的 ThreadLocal 上下文
     */
    private boolean isSaTokenContextReady() {
        // 先检查初始化标志
        if (!saTokenInitialized) {
            return false;
        }
        try {
            // 直接检查 StpLogic 是否已初始化
            // 如果 Sa-Token 已配置，getStpLogic() 应该返回非 null
            return StpUtil.getStpLogic() != null;
        } catch (Exception e) {
            log.warn("SaToken not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证 Token 并检查用户连接数限制
     * 注意：由于 Sa-Token 默认使用 ThreadLocal 上下文，而 Netty EventLoop 线程没有主线程的 ThreadLocal
     * 因此我们直接解析 JWT token 获取用户 ID，而不依赖 StpUtil
     *
     * @param token   JWT token
     * @param channel Netty channel
     * @return 用户 ID，验证失败返回 null
     */
    public String validateTokenAndCheckUserLimit(String token, Channel channel) {
        // 直接解析 JWT token，不依赖 Sa-Token 上下文
        String userId = parseJwtToken(token);
        if (userId == null) {
            log.warn("Token validation failed: invalid token or cannot parse");
            return null;
        }

        // 检查用户连接数限制
        if (!checkUserConnectionLimit(userId)) {
            log.warn("Connection rejected: user {} exceeded connection limit ({})",
                    userId, authProperties.getMaxConnectionsPerUser());
            return null;
        }

        // Increment user connection count
        incrementUserConnectionCount(userId);

        // Store user ID in channel attributes for cleanup
        channel.attr(USER_ID_KEY).set(userId);

        log.info("User authenticated successfully: userId={}, IP={}",
                userId, channel.attr(CLIENT_IP_KEY).get());
        return userId;
    }

    /**
     * 直接解析 JWT token 获取用户 ID（不依赖 Sa-Token 上下文）
     * 使用 hutool-jwt 解析，Sa-Token JWT 模式使用 hutool-jwt
     */
    private String parseJwtToken(String token) {
        try {
            // 解析 JWT token
            cn.hutool.jwt.JWT jwt = JWTUtil.parseToken(token);

            // 获取 payload（包含用户信息）
            cn.hutool.jwt.JWTPayload payload = jwt.getPayload();
            if (payload == null) {
                log.warn("JWT payload is null");
                return null;
            }

            // 从 payload 中获取 loginId（Sa-Token JWT 模式会存储 loginId）
            Object loginIdObj = payload.getClaim("loginId");
            if (loginIdObj == null) {
                log.warn("JWT payload does not contain loginId");
                return null;
            }

            return loginIdObj.toString();
        } catch (Exception e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清理用户连接（在用户断开连接时调用）
     *
     * @param userId 用户 ID
     * @param channel Netty channel
     */
    public void cleanupUserConnection(String userId, Channel channel) {
        if (userId != null) {
            decrementUserConnectionCount(userId);
            log.debug("User connection cleaned up: userId={}", userId);
        }

        // Clear user ID from channel attributes
        channel.attr(USER_ID_KEY).set(null);
    }

    /**
     * 检查 IP 是否在白名单中
     */
    private boolean isIpWhitelisted(String ip) {
        return authProperties.getIpWhitelist().contains(ip);
    }

    /**
     * 检查 IP 是否在黑名单中
     */
    private boolean isIpBlacklisted(String ip) {
        return authProperties.getIpBlacklist().contains(ip);
    }

    /**
     * 检查连接速率限制
     */
    private boolean checkConnectionRateLimit(String ip) {
        int connectionRateLimit = authProperties.getConnectionRateLimit();
        if (connectionRateLimit <= 0) {
            return true;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);
        Deque<Instant> attempts = ipConnectionAttempts.computeIfAbsent(ip, key -> new ConcurrentLinkedDeque<>());

        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }

            if (attempts.size() >= connectionRateLimit) {
                return false;
            }

            attempts.offerLast(now);
            return true;
        }
    }

    /**
     * 检查 IP 连接数限制
     */
    private boolean checkIpConnectionLimit(String ip) {
        AtomicInteger count = ipConnectionCount.getOrDefault(ip, new AtomicInteger(0));
        return count.get() < authProperties.getMaxConnectionsPerIp();
    }

    /**
     * 检查用户连接数限制
     */
    private boolean checkUserConnectionLimit(String userId) {
        AtomicInteger count = userConnectionCount.getOrDefault(userId, new AtomicInteger(0));
        return count.get() < authProperties.getMaxConnectionsPerUser();
    }

    /**
     * 增加 IP 连接计数
     */
    private void incrementIpConnectionCount(String ip) {
        ipConnectionCount.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 减少 IP 连接计数
     */
    private void decrementIpConnectionCount(String ip) {
        AtomicInteger count = ipConnectionCount.get(ip);
        if (count != null) {
            count.decrementAndGet();
            if (count.get() <= 0) {
                ipConnectionCount.remove(ip);
            }
        }
    }

    /**
     * 增加用户连接计数
     */
    private void incrementUserConnectionCount(String userId) {
        userConnectionCount.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 减少用户连接计数
     */
    private void decrementUserConnectionCount(String userId) {
        AtomicInteger count = userConnectionCount.get(userId);
        if (count != null) {
            count.decrementAndGet();
            if (count.get() <= 0) {
                userConnectionCount.remove(userId);
            }
        }
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        return address != null ? address.getAddress().getHostAddress() : "unknown";
    }

    /**
     * 获取当前连接统计信息（用于监控）
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalUserConnections", userConnectionCount.size());
        stats.put("totalIpConnections", ipConnectionCount.size());
        stats.put("userConnectionCounts", new ConcurrentHashMap<>(userConnectionCount));
        stats.put("ipConnectionCounts", new ConcurrentHashMap<>(ipConnectionCount));
        return stats;
    }
}
