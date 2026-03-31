package com.maxxvll.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置属性
 *
 * <p>用于配置加密相关的参数</p>
 *
 * <p>使用方式：
 * <ul>
 *   <li>在配置文件中使用 ENC(...) 包裹加密值</li>
 *   <li>例如：password: ENC(加密后的密文)</li>
 * </ul>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Data
@Component
@ConfigurationProperties(prefix = "jasypt")
public class EncryptedPropertiesConfig {

    /**
     * 加密算法
     */
    private String algorithm = "PBEWithMD5AndDES";

    /**
     * 加密密钥（建议使用环境变量或命令行参数传入）
     */
    private String secret = "${JASYPT_ENCRYPTOR_PASSWORD:changeit}";

    /**
     * 密钥位置
     */
    private String secretLocation;

    /**
     * 是否启用加密
     */
    private boolean enabled = true;

    /**
     * 密钥文件路径（可选）
     */
    private String keyFileLocation;
}
