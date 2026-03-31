package com.maxxvll.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置验证器
 *
 * <p>在应用启动时验证关键配置是否正确</p>
 *
 * <p>使用示例：
 * <ul>
 *   <li>启动时自动验证</li>
 *   <li>缺少必需配置时抛出异常</li>
 *   <li>输出配置摘要信息</li>
 * </ul>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Component
public class ConfigurationValidator {

    private final Environment environment;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${server.port:5050}")
    private int serverPort;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateConfiguration() {
        log.info("========== 配置验证开始 ==========");
        log.info("应用名称: {}", applicationName);
        log.info("激活环境: {}", activeProfile);
        log.info("服务端口: {}", serverPort);

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 验证数据库配置
        validateDatabaseConfig(errors);

        // 验证Redis配置
        validateRedisConfig(errors);

        // 验证敏感配置
        validateSensitiveConfig(errors, warnings);

        // 验证WebSocket配置
        validateWebSocketConfig(errors);

        // 输出警告信息
        if (!warnings.isEmpty()) {
            log.warn("========== 配置警告 ==========");
            for (String warning : warnings) {
                log.warn("- {}", warning);
            }
        }

        // 如果有错误，启动失败
        if (!errors.isEmpty()) {
            log.error("========== 配置错误 ==========");
            for (String error : errors) {
                log.error("- {}", error);
            }
            throw new IllegalStateException(
                    "应用启动失败：发现 " + errors.size() + " 个配置错误，请检查配置文件！");
        }

        log.info("========== 配置验证通过 ==========");
        printConfigurationSummary();
    }

    /**
     * 验证数据库配置
     */
    private void validateDatabaseConfig(List<String> errors) {
        String dbUrl = environment.getProperty("spring.datasource.url");
        String dbUsername = environment.getProperty("spring.datasource.username");
        String dbPassword = environment.getProperty("spring.datasource.password");

        if (dbUrl == null || dbUrl.isEmpty()) {
            errors.add("数据库URL未配置 (spring.datasource.url)");
        }

        if (dbUsername == null || dbUsername.isEmpty()) {
            errors.add("数据库用户名未配置 (spring.datasource.username)");
        }

        if (dbPassword == null || dbPassword.isEmpty()) {
            errors.add("数据库密码未配置 (spring.datasource.password)");
        }

        // 检查是否为生产环境且使用了弱密码
        if ("prod".equals(activeProfile)) {
            if ("root".equals(dbPassword) || "123456".equals(dbPassword)) {
                errors.add("生产环境使用了弱数据库密码，请修改！");
            }
        }
    }

    /**
     * 验证Redis配置
     */
    private void validateRedisConfig(List<String> errors) {
        String redisHost = environment.getProperty("spring.data.redis.host");
        String redisPassword = environment.getProperty("spring.data.redis.password");

        if (redisHost == null || redisHost.isEmpty()) {
            errors.add("Redis主机未配置 (spring.data.redis.host)");
        }

        // 生产环境应配置密码
        if ("prod".equals(activeProfile) && (redisPassword == null || redisPassword.isEmpty())) {
            errors.add("生产环境Redis未配置密码，请配置！");
        }
    }

    /**
     * 验证敏感配置
     */
    private void validateSensitiveConfig(List<String> errors, List<String> warnings) {
        String jwtSecret = environment.getProperty("sa-token.jwt-secret-key");
        String deepseekKey = environment.getProperty("deepseek.api.key");

        // JWT密钥验证
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            errors.add("JWT密钥未配置 (sa-token.jwt-secret-key)");
        } else if (jwtSecret.contains("urfewbfjsdafjk")) {
            warnings.add("JWT密钥使用了默认值，建议修改！");
        } else if (jwtSecret.length() < 32) {
            errors.add("JWT密钥长度不足，至少需要32个字符！");
        }

        // DeepSeek API密钥验证
        if (deepseekKey == null || deepseekKey.isEmpty()) {
            warnings.add("DeepSeek API密钥未配置，AI功能将不可用！");
        } else if (deepseekKey.startsWith("sk-test") || deepseekKey.equals("${DEEPSEEK_API_KEY}")) {
            warnings.add("DeepSeek API密钥使用了测试密钥或环境变量引用！");
        }

        // 生产环境检查
        if ("prod".equals(activeProfile)) {
            String mailPassword = environment.getProperty("spring.mail.password");
            if (mailPassword == null || mailPassword.isEmpty()) {
                warnings.add("生产环境邮箱密码未配置，邮件功能可能不可用！");
            }
        }
    }

    /**
     * 验证WebSocket配置
     */
    private void validateWebSocketConfig(List<String> errors) {
        String wsHost = environment.getProperty("ws.host");
        String wsPort = environment.getProperty("ws.port");

        if (wsHost == null || wsHost.isEmpty()) {
            wsHost = "0.0.0.0"; // 使用默认值
        }

        if (wsPort == null || wsPort.isEmpty()) {
            errors.add("WebSocket端口未配置 (ws.port)");
        }
    }

    /**
     * 打印配置摘要
     */
    private void printConfigurationSummary() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String hostName = InetAddress.getLocalHost().getHostName();
            log.info("========== 运行信息 ==========");
            log.info("主机名: {}", hostName);
            log.info("主机地址: {}", hostAddress);
            log.info("HTTP API: http://{}:{}", hostAddress, serverPort);
            log.info("WebSocket: ws://{}:{}/ws", hostAddress,
                    environment.getProperty("ws.port", "5051"));
            log.info("API文档: http://{}:{}/api/swagger-ui.html", hostAddress, serverPort);
        } catch (UnknownHostException e) {
            log.warn("获取主机信息失败: {}", e.getMessage());
        }
    }

    /**
     * 检查是否为生产环境
     */
    public boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    /**
     * 检查是否为开发环境
     */
    public boolean isDevelopment() {
        return "dev".equals(activeProfile);
    }

    /**
     * 检查是否为测试环境
     */
    public boolean isTest() {
        return "test".equals(activeProfile);
    }
}
