package com.maxxvll.common;

import com.maxxvll.common.event.LoginEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 消费者1：记录登录日志
 */
@Component
public class LoginLogConsumer {

    // 重点：groupId 为 "login-log-group"（唯一）
    @KafkaListener(topics = "login-event-topic", groupId = "login-log-group")
    public void handleLoginEvent(LoginEvent loginEvent) {
        System.out.println("【消费者1-日志服务】收到登录事件：");
        System.out.println("  用户ID：" + loginEvent.getUserId());
        System.out.println("  用户名：" + loginEvent.getUsername());
        System.out.println("  登录时间：" + loginEvent.getLoginTime());
        // 实际业务：将日志写入数据库或文件
    }
}