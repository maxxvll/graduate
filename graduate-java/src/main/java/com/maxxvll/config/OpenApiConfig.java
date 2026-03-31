package com.maxxvll.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 文档配置
 * <p>
 * 配置 Swagger UI 和 OpenAPI JSON 文档的基本信息。
 * 支持 Sa-Token JWT 认证。
 * </p>
 *
 * @author backend-friend
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "JWT认证令牌"
)
@SecurityRequirement(name = "bearerAuth")
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("即时通讯(IM)应用 API")
                        .version("v1.0.0")
                        .description("""
                                即时通讯应用后端API文档。

                                ## 功能模块
                                - **用户认证**：登录、注册、密码修改
                                - **好友管理**：添加好友、好友申请、黑名单
                                - **聊天消息**：发送消息、消息历史、会话管理
                                - **群聊管理**：创建群聊、群成员管理、群文件
                                - **云盘**：文件上传、文件管理、分享链接
                                - **收藏**：消息收藏功能
                                - **语音通话**：WebRTC信令交互

                                ## 认证方式
                                所有需要认证的接口请在请求头中添加：
                                ```
                                satoken: your_jwt_token
                                ```

                                ## 注意事项
                                - 所有请求的 `Content-Type` 为 `application/json`
                                - 所有响应的 `code` 为业务状态码，`message` 为提示信息
                                """)
                        .contact(new Contact()
                                .name("技术支持")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:5050/api")
                                .description("开发环境"),
                        new Server()
                                .url("https://api.example.com")
                                .description("生产环境")
                ));
    }
}
