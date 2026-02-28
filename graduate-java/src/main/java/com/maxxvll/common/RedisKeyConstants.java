package com.maxxvll.common;

public interface RedisKeyConstants {
    /**
     * 分隔符
     */
    String SEPARATOR = ":";
    String USER_LOGIN_FAIL = "login:fail";
    String USER_LOGIN_LOCK = "login:lock";
    // ==================== 用户模块 ====================
    String USER_PREFIX = "user";
    /** 用户信息: user:info:{userId} */
    String USER_INFO = "info";
    /** 扫码登录: user:qr:{qrCodeId} */
    String USER_QR_LOGIN = "qr";

    // ==================== 其他模块示例 ====================
    String CHAT_PREFIX = "chat";
    /** 聊天会话: chat:session:{sessionId} */
    String CHAT_SESSION = "session";
    String USER_CAPTCHA = "captcha";
    /**
     * 构建 Key
     */
    static String buildKey(String prefix, String... items) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String item : items) {
            sb.append(SEPARATOR).append(item);
        }
        return sb.toString();
    }
}
