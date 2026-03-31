package com.maxxvll.service.message.handler;

import com.maxxvll.common.enums.MessageType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息类型处理器注册表
 * 管理所有消息类型处理器，提供根据类型查找处理器的功能
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class MessageTypeHandlerRegistry {

    @Resource
    private List<MessageTypeHandler> handlers;

    /**
     * 根据消息类型获取对应的处理器
     *
     * @param type 消息类型
     * @return 消息处理器，如果没有匹配的则返回null
     */
    public MessageTypeHandler getHandler(MessageType type) {
        return handlers.stream()
                .filter(handler -> handler.supports(type))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取默认的文本消息处理器
     *
     * @return 文本消息处理器
     */
    public MessageTypeHandler getDefaultHandler() {
        return getHandler(MessageType.TEXT);
    }
}
