package com.maxxvll.netty;

import io.netty.util.AttributeKey;

/**
 * WebSocket 常量定义
 *
 * @author backend-friend
 */
public class WebSocketConstants {

    /**
     * 用户ID AttributeKey
     */
    public static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");

    /**
     * 设备类型 AttributeKey
     */
    public static final AttributeKey<String> DEVICE_TYPE_KEY = AttributeKey.valueOf("deviceType");

    /**
     * 客户端IP AttributeKey
     */
    public static final AttributeKey<String> CLIENT_IP_KEY = AttributeKey.valueOf("clientIp");

    /**
     * 未知设备类型
     */
    public static final String UNKNOWN_DEVICE_TYPE = "unknown";

    /**
     * 消息类型常量
     */
    public static final class MessageType {
        public static final String PING = "ping";
        public static final String PONG = "pong";
        public static final String CHAT = "chat";
        public static final String SIGNAL = "signal";
        public static final String TYPING = "typing";
        public static final String ACK = "ack";
        public static final String ERROR = "error";
        public static final String AUTH_ERROR = "auth_error";
        public static final String CONNECTED = "connected";
        public static final String MESSAGE = "message";
        public static final String READ_SYNC = "read_sync";
    }

    private WebSocketConstants() {
    }
}
