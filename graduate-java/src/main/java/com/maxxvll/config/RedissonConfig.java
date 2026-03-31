package com.maxxvll.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.ConstantDelay;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RedissonConfig {

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

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setPassword(redisPassword == null || redisPassword.isBlank() ? null : redisPassword);
        config.setTcpKeepAlive(true);
        config.setTcpNoDelay(true);

        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(String.format("redis://%s:%d", redisHost, redisPort));
        singleServerConfig.setDatabase(redisDatabase);
        singleServerConfig.setTimeout(redisTimeout);
        singleServerConfig.setConnectionPoolSize(50);
        singleServerConfig.setConnectionMinimumIdleSize(5);
        singleServerConfig.setIdleConnectionTimeout(180_000);
        singleServerConfig.setRetryAttempts(3);
        singleServerConfig.setRetryDelay(new ConstantDelay(Duration.ofMillis(1_500)));
        singleServerConfig.setConnectTimeout(10_000);

        return Redisson.create(config);
    }
}
