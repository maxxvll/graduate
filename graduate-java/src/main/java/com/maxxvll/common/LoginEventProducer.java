package com.maxxvll.common;


import com.maxxvll.common.event.LoginEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 登录事件生产者
 */
@Service
public class LoginEventProducer {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    // 登录事件主题（建议抽成常量）
    private static final String LOGIN_EVENT_TOPIC = "login-event-topic";

    /**
     * 发送登录事件
     * @param loginEvent 登录事件对象
     */
    public void sendLoginEvent(LoginEvent loginEvent) {
        kafkaTemplate.send(LOGIN_EVENT_TOPIC, loginEvent);
        System.out.println("【生产者】发送登录事件：" + loginEvent);
    }
}