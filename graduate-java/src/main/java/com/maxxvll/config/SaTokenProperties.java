package com.maxxvll.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 配置属性
 * 从配置文件读取白名单等配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "satoken")
public class SaTokenProperties {

    /**
     * 是否开启认证
     */
    private Boolean enabled = true;

    /**
     * 认证排除路径（白名单）
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 认证包含路径（黑名单，可选）
     */
    private List<String> includePaths = new ArrayList<>();

    /**
     * 是否允许跨域
     */
    private Boolean allowCrossOrigin = true;

    /**
     * 跨域允许的源
     */
    private String allowOrigin = "http://localhost:5100";

    /**
     * Token 过期时间（秒）
     */
    private Long timeout = 2592000L;

    /**
     * 是否允许并发登录
     */
    private Boolean isConcurrent = true;

    /**
     * Token 风格
     */
    private String tokenStyle = "uuid";
}
