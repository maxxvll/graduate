package com.maxxvll;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 应用上下文加载测试
 */
@SpringBootTest(classes = Main.class)
@ActiveProfiles("test")
@Disabled("Requires external infrastructure (MySQL, Redis, Kafka, MinIO)")
class GraduateJavaApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring Boot 应用上下文可以正常加载
    }
}
