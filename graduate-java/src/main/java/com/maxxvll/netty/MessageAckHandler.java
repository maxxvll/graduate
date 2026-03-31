package com.maxxvll.netty;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
@ChannelHandler.Sharable
public class MessageAckHandler extends ChannelOutboundHandlerAdapter {

    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP = new TypeReference<>() {
    };

    /**
     * 统计指标
     */
    private final AtomicLong totalAckMessages = new AtomicLong(0);
    private final AtomicLong totalAckReceived = new AtomicLong(0);
    private final AtomicLong totalAckTimeout = new AtomicLong(0);

    /**
     * ACK 消息类型
     */
    private static final String TYPE_CHAT = "chat";
    private static final String TYPE_SIGNAL = "signal";

    @Value("${ws.ack.enabled:true}")
    private boolean ackEnabled;

    @Value("${ws.ack.timeout-ms:5000}")
    private long ackTimeoutMs;

    @Value("${ws.ack.max-retries:3}")
    private int maxRetries;

    @Resource
    private PendingMessageManager pendingMessageManager;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof TextWebSocketFrame frame)) {
            super.write(ctx, msg, promise);
            return;
        }

        // 如果 ACK 功能被禁用，直接发送原始消息
        if (!ackEnabled) {
            super.write(ctx, msg, promise);
            return;
        }

        String content = frame.text();
        try {
            Map<String, Object> message = JSON.parseObject(content, STRING_OBJECT_MAP);
            String type = getString(message, "type");

            if (!needsAck(type)) {
                super.write(ctx, msg, promise);
                return;
            }

            String targetUserId = getTargetUserId(message);
            if (targetUserId == null || targetUserId.isBlank()) {
                log.debug("Skip ACK decoration because target user is missing, type={}", type);
                super.write(ctx, msg, promise);
                return;
            }

            String messageId = UUID.randomUUID().toString();
            message.put("messageId", messageId);
            message.put("requireAck", true);

            String modifiedContent = JSON.toJSONString(message);
            pendingMessageManager.addPendingMessage(messageId, targetUserId, modifiedContent, type);
            totalAckMessages.incrementAndGet();
            super.write(ctx, new TextWebSocketFrame(modifiedContent), promise);
            return;
        } catch (Exception e) {
            log.error("Decorate outbound ACK message failed, send original content instead", e);
        }

        super.write(ctx, msg, promise);
    }

    public void handleAck(ChannelHandlerContext ctx, Map<String, Object> ackMessage) {
        String messageId = getString(ackMessage, "messageId");
        if (messageId == null || messageId.isBlank()) {
            log.warn("Received ACK without messageId");
            return;
        }

        boolean success = pendingMessageManager.acknowledgeMessage(messageId);
        if (success) {
            totalAckReceived.incrementAndGet();
            sendAckConfirmation(ctx, messageId);
        } else {
            log.warn("ACK did not match a pending message, messageId={}", messageId);
        }
    }

    private void sendAckConfirmation(ChannelHandlerContext ctx, String messageId) {
        Map<String, Object> confirmation = new HashMap<>();
        confirmation.put("type", "ack_confirmed");
        confirmation.put("messageId", messageId);
        confirmation.put("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(confirmation)));
    }

    private boolean needsAck(String type) {
        return TYPE_CHAT.equals(type) || TYPE_SIGNAL.equals(type);
    }

    /**
     * 获取统计摘要
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAckMessages", totalAckMessages.get());
        stats.put("totalAckReceived", totalAckReceived.get());
        stats.put("totalAckTimeout", totalAckTimeout.get());
        stats.put("ackEnabled", ackEnabled);
        stats.put("ackTimeoutMs", ackTimeoutMs);
        stats.put("maxRetries", maxRetries);
        if (totalAckMessages.get() > 0) {
            stats.put("ackSuccessRate", String.format("%.2f%%",
                    (double) totalAckReceived.get() / totalAckMessages.get() * 100));
        } else {
            stats.put("ackSuccessRate", "N/A");
        }
        return stats;
    }

    private String getTargetUserId(Map<String, Object> message) {
        Object data = message.get("data");
        if (!(data instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Object targetUserId = rawMap.get("targetUserId");
        return targetUserId instanceof String text ? text : null;
    }

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof String text ? text : null;
    }
}
