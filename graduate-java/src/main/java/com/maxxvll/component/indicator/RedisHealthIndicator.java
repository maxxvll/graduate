package com.maxxvll.component.indicator;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Redis 健康检查指示器
 * 集成到 Spring Boot Actuator，通过 /actuator/health 端点检查 Redis 服务状态
 */
@Slf4j
@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Health health() {
        try {
            // 尝试执行一个简单的ping命令来验证连接
            String pingResult = redissonClient.getBucket("health:ping").get() != null ? "pong" : "ok";

            Health.Builder builder = Health.up()
                    .withDetail("status", "Redis服务运行正常")
                    .withDetail("client", "Redisson")
                    .withDetail("ping", pingResult);

            return builder.build();

        } catch (Exception e) {
            log.error("Redis健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Redis服务异常")
                    .build();
        }
    }
}
