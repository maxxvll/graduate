package com.maxxvll.netty;

import com.maxxvll.component.NettyChannelManager;
import jakarta.annotation.Resource;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WebSocket 健康检查指示器
 * 集成到 Spring Boot Actuator，通过 /actuator/health 端点检查 WebSocket 服务状态
 */
@Component
public class WebSocketHealthIndicator implements HealthIndicator {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Override
    public Health health() {
        try {
            Map<String, Object> metrics = nettyChannelManager.getMetrics();
            Integer activeConnections = (Integer) metrics.get("activeConnections");

            // 构建健康状态
            Health.Builder builder = Health.up()
                    .withDetail("activeConnections", activeConnections)
                    .withDetail("totalConnections", metrics.get("totalConnections"))
                    .withDetail("messagesSent", metrics.get("messagesSent"))
                    .withDetail("messagesReceived", metrics.get("messagesReceived"))
                    .withDetail("connectionsPerDevice", metrics.get("connectionsPerDevice"))
                    .withDetail("status", "WebSocket服务运行正常");

            // 如果活跃连接数超过10000，添加警告信息
            if (activeConnections > 10000) {
                builder.withDetail("warning", "活跃连接数超过10000，建议进行扩容");
            }

            return builder.build();

        } catch (Exception e) {
            // 如果获取指标失败，返回DOWN状态
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "WebSocket服务异常")
                    .build();
        }
    }
}
