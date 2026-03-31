package com.maxxvll.netty.handler;

import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.netty.WebSocketConstants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 心跳消息处理器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class HeartbeatMessageHandler implements WebSocketMessageHandler {

    private static final String HEARTBEAT_PONG_TEMPLATE;

    static {
        Map<String, Object> pongTemplate = new HashMap<>(3);
        pongTemplate.put("type", "pong");
        pongTemplate.put("data", "heartbeat");
        HEARTBEAT_PONG_TEMPLATE = com.alibaba.fastjson2.JSON.toJSONString(pongTemplate);
    }

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Override
    public String getType() {
        return WebSocketConstants.MessageType.PING;
    }

    @Override
    public int getOrder() {
        return 10; // 高优先级
    }

    @Override
    public boolean handle(ChannelHandlerContext ctx, Map<String, Object> data) {
        String userId = ctx.channel().attr(WebSocketConstants.USER_ID_KEY).get();
        if (userId != null) {
            nettyChannelManager.recordHeartbeatReceived(userId);
        }

        String pongMessage = appendTimestamp(HEARTBEAT_PONG_TEMPLATE);
        ctx.writeAndFlush(new TextWebSocketFrame(pongMessage));
        return true;
    }

    private String appendTimestamp(String template) {
        return template.replace("}", ",\"timestamp\":" + System.currentTimeMillis() + "}");
    }
}
