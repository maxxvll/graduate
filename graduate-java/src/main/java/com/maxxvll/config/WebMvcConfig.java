package com.maxxvll.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maxxvll.common.interceptor.LogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC 配置类
 * 解决"No acceptable representation"内容协商问题
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 日志拦截器（自动注入）
     */
    private final LogInterceptor logInterceptor;

    public WebMvcConfig(LogInterceptor logInterceptor) {
        this.logInterceptor = logInterceptor;
    }

    /**
     * 注册日志拦截器
     * <p>
     * 拦截所有请求，自动管理 MDC 上下文和记录请求日志
     * </p>
     * <p>
     * 排除路径：
     * <ul>
     *     <li>/error - 错误页面</li>
     *     <li>/actuator/** - 健康检查端点</li>
     *     <li>/swagger-ui/** - Swagger UI</li>
     *     <li>/v3/api-docs/** - API 文档</li>
     * </ul>
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }

    /**
     * 配置跨域支持
     * 说明：
     * - 允许所有来源（包括 localhost、127.0.0.1、IP地址等）
     * - 支持预检请求 (preflight)
     * - 支持凭证传递（cookies、authorization）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 对于开发环境和生产环境都友好的配置
                .allowedOrigins(
                    "http://localhost:5100",      // 本地开发 (localhost)
                    "http://127.0.0.1:5100",      // 本地开发 (127.0.0.1)
                    "http://192.168.145.1:5100",  // 本地网络 IP
                    "http://47.99.57.75:5100",    // 公网 IP
                    "https://47.99.57.75:5100"    // HTTPS
                )
                // 如果需要支持更多来源，使用下面这行（但需要谨慎 - 生产环境慎用）
                // .allowedOriginPatterns(".*") // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置内容协商，确保JSON响应正常工作
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .mediaType("json", org.springframework.http.MediaType.APPLICATION_JSON)
                .mediaType("xml", org.springframework.http.MediaType.APPLICATION_XML)
                .mediaType("*/*", org.springframework.http.MediaType.APPLICATION_JSON);
    }

    /**
     * 配置消息转换器，确保JSON序列化正常工作
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 添加字符串转换器
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        
        // 添加Jackson JSON转换器
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        jsonConverter.setObjectMapper(objectMapper);
        converters.add(jsonConverter);
    }
}