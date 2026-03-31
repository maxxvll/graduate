package com.maxxvll.netty;

import com.maxxvll.netty.handler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 消息处理器分发器
 * 负责将消息分发到对应的处理器
 *
 * @author backend-friend
 */
@Slf4j
@Component
public class WebSocketMessageDispatcher {

    @Resource
    private List<WebSocketMessageHandler> messageHandlers;

    private Map<String, WebSocketMessageHandler> handlerMap;

    /**
     * 初始化处理器映射
     */
    public void initHandlers() {
        if (handlerMap != null) {
            return;
        }

        handlerMap = new HashMap<>();
        messageHandlers.stream()
                .sorted(Comparator.comparingInt(WebSocketMessageHandler::getOrder))
                .forEach(handler -> {
                    String type = handler.getType();
                    if (handlerMap.containsKey(type)) {
                        log.warn("Duplicate message handler for type: {}, existing handler will be overridden", type);
                    }
                    handlerMap.put(type, handler);
                    log.debug("Registered WebSocket message handler: type={}, class={}", type, handler.getClass().getSimpleName());
                });

        log.info("WebSocket message dispatcher initialized, handlers={}", handlerMap.keySet());
    }

    /**
     * 分发消息到对应的处理器
     *
     * @param ctx ChannelHandlerContext
     * @param type 消息类型
     * @param data 消息数据
     * @return 是否处理成功
     */
    public boolean dispatch(ChannelHandlerContext ctx, String type, Map<String, Object> data) {
        if (handlerMap == null) {
            initHandlers();
        }

        WebSocketMessageHandler handler = handlerMap.get(type);
        if (handler == null) {
            log.warn("Unknown websocket message type: {}", type);
            sendError(ctx, "Unknown message type: " + type);
            return false;
        }

        try {
            return handler.handle(ctx, data);
        } catch (Exception e) {
            log.error("Failed to handle websocket message, type={}", type, e);
            sendError(ctx, "Failed to process message: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有已注册的处理器类型
     *
     * @return 处理器类型集合
     */
    public java.util.Set<String> getSupportedTypes() {
        if (handlerMap == null) {
            initHandlers();
        }
        return handlerMap.keySet();
    }

    /**
     * 发送错误消息
     */
    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", WebSocketConstants.MessageType.ERROR);
        error.put("message", errorMessage);
        error.put("timestamp", System.currentTimeMillis());
        ctx.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(
                com.alibaba.fastjson2.JSON.toJSONString(error)));
    }
}
