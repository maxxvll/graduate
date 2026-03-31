package com.maxxvll.component.indicator;

import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * MinIO 健康检查指示器
 * 集成到 Spring Boot Actuator，通过 /actuator/health 端点检查 MinIO 服务状态
 */
@Slf4j
@Component
public class MinioHealthIndicator implements HealthIndicator {

    @Resource
    private MinioClient minioClient;

    @Override
    public Health health() {
        try {
            // 尝试获取存储桶列表来验证连接
            boolean bucketExists = minioClient.bucketExists(
                io.minio.BucketExistsArgs.builder()
                    .bucket("file-storage-bucket")
                    .build()
            );

            return Health.up()
                    .withDetail("status", "MinIO服务运行正常")
                    .withDetail("defaultBucket", "file-storage-bucket")
                    .withDetail("bucketAccessible", bucketExists)
                    .build();

        } catch (Exception e) {
            log.error("MinIO健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "MinIO服务异常")
                    .build();
        }
    }
}
