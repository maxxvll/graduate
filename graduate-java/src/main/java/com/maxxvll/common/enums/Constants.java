package com.maxxvll.common.enums;

/**
 * @deprecated 已废弃，请使用具体的常量类
 * <ul>
 *   <li>WebSocket Token: {@link com.maxxvll.common.constants.RedisKeyConstants#USER_WS_TOKEN}</li>
 * </ul>
 */
@Deprecated
public class Constants {

    /**
     * @deprecated 请使用 {@link com.maxxvll.common.constants.RedisKeyConstants#USER_WS_TOKEN}
     */
    @Deprecated
    public static final String REDIS_KEY_WS_TOKEN = "chat:user:ws:token:";

    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
