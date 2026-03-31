package com.maxxvll.netty;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.netty.handler.WebSocketMessageHandler;
import com.maxxvll.utils.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Netty WebSocket 消息处理器
 * 负责处理接收到的 WebSocket 消息
 *
 * @author backend-friend
 */
@Component
@ChannelHandler.Sharable
@Slf4j
public class NettyManagerHandle extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP = new TypeReference<>() {
    };

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private WebSocketAuthInterceptor authInterceptor;

    @Resource
    private MessageAckHandler messageAckHandler;

    @Resource
    private PendingMessageManager pendingMessageManager;

    @Resource
    private WebSocketMessageDispatcher messageDispatcher;

    @Resource(name = "nettyBusinessExecutor")
    private ExecutorService nettyBusinessExecutor;

    @PostConstruct
    public void init() {
        // 初始化消息处理器分发器
        messageDispatcher.initHandlers();
        log.info("NettyManagerHandle initialized");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("WebSocket channel active, remoteAddress={}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String messageText = frame.text();
        nettyChannelManager.recordMessageReceived();
        log.debug("Received websocket message: {}", messageText);

        try {
            nettyBusinessExecutor.execute(() -> processMessage(ctx, messageText));
        } catch (RejectedExecutionException e) {
            log.warn("Reject websocket message because business executor is full, remoteAddress={}",
                    ctx.channel().remoteAddress());
            sendError(ctx, "Server is busy, please retry later.");
        }
    }

    /**
     * 处理接收到的消息
     */
    private void processMessage(ChannelHandlerContext ctx, String messageText) {
        try {
            Map<String, Object> message = parseMessage(messageText);
            String type = extractString(message, "type");

            if (type == null) {
                sendError(ctx, "Message type is required.");
                return;
            }

            // 使用消息处理器分发器处理消息
            Object data = message.get("data");
            if (data instanceof Map<?, ?> rawData) {
                Map<String, Object> dataMap = toStringObjectMap(rawData);
                messageDispatcher.dispatch(ctx, type, dataMap);
            } else {
                messageDispatcher.dispatch(ctx, type, Map.of("data", data));
            }
        } catch (Exception e) {
            log.error("Handle websocket message failed", e);
            sendError(ctx, "Failed to process message: " + e.getMessage());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            handleIdleStateEvent(ctx, idleStateEvent);
            return;
        }

        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete) {
            handleHandshakeComplete(ctx, handshakeComplete.requestUri());
        }

        super.userEventTriggered(ctx, evt);
    }

    /**
     * 处理空闲状态事件
     */
    private void handleIdleStateEvent(ChannelHandlerContext ctx, IdleStateEvent idleStateEvent) {
        if (idleStateEvent.state() == IdleState.READER_IDLE) {
            log.warn("Close idle websocket connection, userId={}",
                    ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get());
            ctx.close();
        } else if (idleStateEvent.state() == IdleState.ALL_IDLE) {
            // 发送心跳包
            Map<String, Object> pingMessage = new HashMap<>();
            pingMessage.put("type", "ping");
            pingMessage.put("data", "heartbeat");
            pingMessage.put("timestamp", System.currentTimeMillis());
            ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(pingMessage)));
        }
    }

    /**
     * 处理 WebSocket 握手完成事件
     */
    private void handleHandshakeComplete(ChannelHandlerContext ctx, String requestUri) {
        String token = readQueryParam(requestUri, "token");
        if (StringTools.isEmpty(token)) {
            log.warn("Close websocket connection because token is missing");
            sendAuthError(ctx, "Token is required.");
            ctx.close();
            return;
        }

        String deviceType = readQueryParam(requestUri, "deviceType");
        final String finalDeviceType = StringTools.isEmpty(deviceType)
                ? WebSocketConstants.UNKNOWN_DEVICE_TYPE
                : deviceType;

        String userId = authInterceptor.validateTokenAndCheckUserLimit(token, ctx.channel());
        if (userId == null) {
            log.warn("Close websocket connection because auth failed");
            sendAuthError(ctx, "Token validation failed or connection limit exceeded.");
            ctx.close();
            return;
        } else if (WebSocketAuthInterceptor.RETRY_MARKER.equals(userId)) {
            scheduleAuthRetry(ctx, token, finalDeviceType);
            return;
        }

        completeHandshake(ctx, userId, finalDeviceType);
    }

    /**
     * 安排认证重试
     */
    private void scheduleAuthRetry(ChannelHandlerContext ctx, String token, String deviceType) {
        log.warn("SaToken not ready, scheduling retry for client");
        ctx.channel().eventLoop().schedule(() -> {
            String retryUserId = authInterceptor.validateTokenAndCheckUserLimit(token, ctx.channel());
            if (retryUserId != null && !WebSocketAuthInterceptor.RETRY_MARKER.equals(retryUserId)) {
                completeHandshake(ctx, retryUserId, deviceType);
            } else {
                log.warn("Retry failed, close connection");
                sendAuthError(ctx, "Server not ready, please login again.");
                ctx.close();
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 完成 WebSocket 握手并绑定用户
     */
    private void completeHandshake(ChannelHandlerContext ctx, String userId, String deviceType) {
        ctx.channel().attr(WebSocketConstants.USER_ID_KEY).set(userId);
        ctx.channel().attr(WebSocketConstants.DEVICE_TYPE_KEY).set(deviceType);
        nettyChannelManager.bindChannel(userId, ctx.channel(), deviceType);

        log.info("WebSocket handshake completed, userId={}, deviceType={}, remoteAddress={}",
                userId, deviceType, ctx.channel().remoteAddress());
        sendConnectionSuccessMessage(ctx, userId);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String userId = ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get();
        String deviceType = ctx.channel().attr(WebSocketConstants.DEVICE_TYPE_KEY).get();

        if (!StringTools.isEmpty(userId)) {
            // 只移除断开的那个 channel，不影响其他设备
            boolean fullyOffline = nettyChannelManager.removeChannel(userId, ctx.channel());

            // 仅在用户完全离线时清理心跳数据和 pending messages
            if (fullyOffline) {
                nettyChannelManager.cleanupUserHeartbeatData(userId);
                pendingMessageManager.clearUserPendingMessages(userId);
            }
            authInterceptor.cleanupUserConnection(userId, ctx.channel());
            log.info("WebSocket channel inactive, userId={}, deviceType={}, fullyOffline={}, remoteAddress={}",
                    userId, deviceType, fullyOffline, ctx.channel().remoteAddress());
        } else {
            log.info("WebSocket channel inactive before auth, remoteAddress={}", ctx.channel().remoteAddress());
        }

        ctx.channel().attr(WebSocketConstants.USER_ID_KEY).set(null);
        ctx.channel().attr(WebSocketConstants.DEVICE_TYPE_KEY).set(null);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket channel error, remoteAddress={}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    // ==================== 辅助方法 ====================

    private void sendConnectionSuccessMessage(ChannelHandlerContext ctx, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", WebSocketConstants.MessageType.CONNECTED);
        message.put("userId", userId);
        message.put("timestamp", System.currentTimeMillis());
        message.put("message", "WebSocket connected.");
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(message)));
    }

    private void sendAuthError(ChannelHandlerContext ctx, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", WebSocketConstants.MessageType.AUTH_ERROR);
        error.put("message", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(error)));
    }

    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", WebSocketConstants.MessageType.ERROR);
        error.put("message", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(error)));
    }

    public void checkHeartbeatAnomalies() {
        Map<String, Object> heartbeatStats = nettyChannelManager.getHeartbeatStats();
        Double avgResponseTime = heartbeatStats.get("avgResponseTime") instanceof Number number
                ? number.doubleValue()
                : null;
        if (avgResponseTime == null || avgResponseTime <= 0) {
            return;
        }

        double threshold = avgResponseTime * 2;
        nettyChannelManager.getOnlineUsersSnapshot().forEach(userId -> {
            Long responseTime = nettyChannelManager.getHeartbeatResponseTime(userId);
            if (responseTime != null && responseTime > threshold) {
                log.warn("Detected heartbeat anomaly, userId={}, responseTime={}ms, avg={}ms, threshold={}ms",
                        userId, responseTime, avgResponseTime, threshold);
            }
        });
    }

    private Map<String, Object> parseMessage(String messageText) {
        if (messageText == null || messageText.isBlank()) {
            return Map.of();
        }
        return JSON.parseObject(messageText, STRING_OBJECT_MAP);
    }

    private Map<String, Object> toStringObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>(rawMap.size());
        rawMap.forEach((key, mapValue) -> {
            if (key != null) {
                result.put(key.toString(), mapValue);
            }
        });
        return result;
    }

    private String extractString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof String text ? text : null;
    }

    private String readQueryParam(String url, String key) {
        if (StringTools.isEmpty(url) || !url.contains("?")) {
            return null;
        }

        String[] queryParts = url.split("\\?", 2);
        if (queryParts.length < 2 || StringTools.isEmpty(queryParts[1])) {
            return null;
        }

        String[] params = queryParts[1].split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && key.equals(keyValue[0])) {
                return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}

