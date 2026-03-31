package com.maxxvll.netty.handler;

import com.maxxvll.netty.WebSocketConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 信令消息处理器（用于WebRTC等）
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class SignalingMessageHandler implements WebSocketMessageHandler {

    @Override
    public String getType() {
        return "signal";
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, Map<String, Object> data) {
        String userId = ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get();
        if (userId == null) {
            sendError(ctx, "Unauthorized.");
            return false;
        }

        String targetUserId = extractString(data, "targetUserId");
        String signalType = extractString(data, "signalType");

        log.info("Received signaling message, from={}, to={}, type={}", userId, targetUserId, signalType);
        if (targetUserId != null) {
            Map<String, Object> forwardData = new HashMap<>(data);
            forwardData.put("fromUserId", userId);
            forwardMessageToUser(targetUserId, forwardData);
        }
        return true;
    }

    protected void forwardMessageToUser(String targetUserId, Map<String, Object> data) {
        // 由具体的转发逻辑实现
    }

    protected String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof String text ? text : null;
    }

    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("message", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new TextWebSocketFrame(com.alibaba.fastjson2.JSON.toJSONString(error)));
    }
}
