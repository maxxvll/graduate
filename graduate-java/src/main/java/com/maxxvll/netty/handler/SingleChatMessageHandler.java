package com.maxxvll.netty.handler;

import com.maxxvll.component.NettyChannelManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 单聊消息处理器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class SingleChatMessageHandler extends ChatMessageHandler {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Override
    protected boolean handleChatMessage(ChannelHandlerContext ctx, String userId, Map<String, Object> data) {
        String targetUserId = extractString(data, "targetUserId");
        if (targetUserId == null) {
            log.warn("Missing targetUserId in chat message, userId={}", userId);
            return false;
        }

        forwardMessageToUser(targetUserId, data);
        return true;
    }

    protected void forwardMessageToUser(String targetUserId, Object data) {
        Channel targetChannel = nettyChannelManager.getChannel(targetUserId);
        if (targetChannel == null || !targetChannel.isActive()) {
            log.debug("Skip forward because target user is offline, userId={}", targetUserId);
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("type", "message");
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        targetChannel.writeAndFlush(new TextWebSocketFrame(com.alibaba.fastjson2.JSON.toJSONString(message)));
    }

    protected String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value instanceof String text ? text : null;
    }
}
