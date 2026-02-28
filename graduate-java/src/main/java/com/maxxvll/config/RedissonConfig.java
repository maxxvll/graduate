package com.maxxvll.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类（适配你的application.yml配置）
 * 核心：读取spring.data.redis下的host/port/password等配置，而非url
 */
@Configuration
public class RedissonConfig {

    // 读取spring.data.redis下的配置（完全匹配你的yml）
    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.data.redis.timeout:5000}")
    private int redisTimeout;

    /**
     * 创建RedissonClient实例（适配你的Redis配置）
     */
    @Bean(destroyMethod = "shutdown") // 容器销毁时自动关闭
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 拼接Redis地址：redis://host:port（适配你的host+port配置）
        String redisAddress = String.format("redis://%s:%d", redisHost, redisPort);

        // 单机模式配置（完全匹配你的Redis参数）
        config.useSingleServer()
                .setAddress(redisAddress)
                // 处理密码：空字符串转为null（Redisson要求无密码时传null）
                .setPassword(redisPassword.isBlank() ? null : redisPassword)
                .setDatabase(redisDatabase)
                .setTimeout(redisTimeout) // 连接超时时间（匹配你的5000ms）
                .setConnectionPoolSize(50) // 连接池大小（匹配你的lettuce.pool.max-active=50）
                .setConnectionMinimumIdleSize(5) // 最小空闲连接（匹配你的lettuce.pool.min-idle=5）
                .setIdleConnectionTimeout(180000) // 空闲连接超时（参考你的datasource配置）
                .setRetryAttempts(3); // 连接失败重试次数（新增，提升容错）

        return Redisson.create(config);
    }
}