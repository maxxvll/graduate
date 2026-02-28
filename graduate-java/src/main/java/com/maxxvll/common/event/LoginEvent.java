package com.maxxvll.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录事件对象（Kafka消息载体）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginEvent {
    private Long userId;          // 用户ID
    private String username;      // 用户名
    private LocalDateTime loginTime; // 登录时间
    private String clientIp;      // 客户端IP
}