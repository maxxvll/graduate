package com.maxxvll;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.maxxvll.mapper")
@EnableAsync  // 启用异步支持，用于事件驱动的异步处理
@EnableScheduling  // 启用定时任务支持，用于心跳监控和消息重试

public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class);
    }
}

