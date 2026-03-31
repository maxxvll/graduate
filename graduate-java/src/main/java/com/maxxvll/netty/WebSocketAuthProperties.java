package com.maxxvll.netty;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 认证配置属性
 * 从 application.yaml 中读取 ws.auth.* 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ws.auth")
public class WebSocketAuthProperties {

    /**
     * 每个用户最大连接数（防止多开）
     */
    private int maxConnectionsPerUser = 3;

    /**
     * 每个 IP 最大连接数（防止连接洪水攻击）
     */
    private int maxConnectionsPerIp = 10;

    /**
     * 每个 IP 每分钟最大连接数（连接速率限制）
     */
    private int connectionRateLimit = 10;

    /**
     * IP 白名单（空列表表示不限制）
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * IP 黑名单（黑名单中的 IP 无法连接）
     */
    private List<String> ipBlacklist = new ArrayList<>();

    /**
     * 是否启用 IP 白名单（true 时只有白名单内的 IP 可以连接）
     */
    private boolean enableIpWhitelist = false;

    /**
     * 是否启用 Token 过期检测
     */
    private boolean enableTokenExpirationCheck = true;

    /**
     * Token 过期缓冲时间（秒）
     * 在 Token 真正过期前 N 秒就认为其已过期
     */
    private int tokenExpirationBuffer = 300; // 默认 5 分钟
}
