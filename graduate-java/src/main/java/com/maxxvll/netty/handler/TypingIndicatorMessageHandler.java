package com.maxxvll.netty.handler;

import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.netty.WebSocketConstants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 正在输入指示器处理器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class TypingIndicatorMessageHandler implements WebSocketMessageHandler {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Override
    public String getType() {
        return WebSocketConstants.MessageType.TYPING;
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, Map<String, Object> data) {
        String userId = ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get();
        if (userId == null) {
            return false;
        }

        String targetUserId = extractString(data, "targetUserId");
        Boolean isTyping = extractBoolean(data, "isTyping");
        if (targetUserId != null && isTyping != null) {
            Map<String, Object> forwardData = new HashMap<>();
            forwardData.put("type", "typing");
            forwardData.put("fromUserId", userId);
            forwardData.put("isTyping", isTyping);
            forwardMessageToUser(targetUserId, forwardData);
        }
        return true;
    }

    protected void forwardMessageToUser(String targetUserId, Map<String, Object> data) {
        Channel targetChannel = nettyChannelManager.getChannel(targetUserId);
        if (targetChannel == null || !targetChannel.isActive()) {
            log.debug("Skip typing indicator because target user is offline, userId={}", targetUserId);
            return;
        }

        targetChannel.writeAndFlush(new TextWebSocketFrame(com.alibaba.fastjson2.JSON.toJSONString(data)));
    }

    protected String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof String text ? text : null;
    }

    protected Boolean extractBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }
}
