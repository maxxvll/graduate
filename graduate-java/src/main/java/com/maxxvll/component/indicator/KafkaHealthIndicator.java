package com.maxxvll.component.indicator;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Kafka 健康检查指示器
 * 集成到 Spring Boot Actuator，通过 /actuator/health 端点检查 Kafka 服务状态
 */
@Slf4j
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    @Resource
    private KafkaAdmin kafkaAdmin;

    private static final int TIMEOUT_SECONDS = 5;

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult cluster = adminClient.describeCluster();

            String clusterId = cluster.clusterId().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();

            return Health.up()
                    .withDetail("status", "Kafka服务运行正常")
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .build();

        } catch (Exception e) {
            log.error("Kafka健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Kafka服务异常")
                    .build();
        }
    }
}
