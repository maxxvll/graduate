package com.maxxvll.common.event;

import java.time.LocalDateTime;

/**
 * 登录事件对象（Kafka消息载体）
 */
public class LoginEvent {
    private Long userId;          // 用户ID
    private String username;      // 用户名
    private LocalDateTime loginTime; // 登录时间
    private String clientIp;      // 客户端IP

    public LoginEvent() {
    }

    public LoginEvent(Long userId, String username, LocalDateTime loginTime, String clientIp) {
        this.userId = userId;
        this.username = username;
        this.loginTime = loginTime;
        this.clientIp = clientIp;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public String getClientIp() {
        return clientIp;
    }
}
