package com.maxxvll.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 认证配置
 *
 * 配置说明：
 * - 使用 JWT 简单模式
 * - 白名单通过配置文件 satoken.exclude-paths 配置
 * - 支持跨域配置
 * - 安全头配置
 *
 * @author Claude Code
 */
@Slf4j
@Configuration
public class SaTokenConfigure {

    @Resource
    private SaTokenProperties saTokenProperties;

    /**
     * 使用 JWT 简单模式
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * Sa-Token Servlet 过滤器
     * 配置认证白名单、跨域、安全头等
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        SaServletFilter filter = new SaServletFilter();

        // 配置包含路径
        if (!saTokenProperties.getIncludePaths().isEmpty()) {
            filter.addInclude(saTokenProperties.getIncludePaths().toArray(new String[0]));
        } else {
            filter.addInclude("/**");
        }

        // 配置排除路径（白名单）
        configureExcludePaths(filter);

        // 配置认证逻辑
        filter.setAuth(obj -> {
            log.debug("Sa-Token 全局认证检查");
            SaRouter.match("/**", () -> StpUtil.checkLogin());
        });

        // 配置异常处理
        filter.setError(e -> {
            log.warn("Sa-Token 认证异常: {}", e.getMessage());
            return SaResult.error(e.getMessage());
        });

        // 配置请求前处理（跨域、安全头）
        filter.setBeforeAuth(r -> {
            configureHeaders();
            handleOptionsRequest();
        });

        log.info("Sa-Token 配置初始化完成，白名单路径: {}", saTokenProperties.getExcludePaths());
        return filter;
    }

    /**
     * 配置排除路径（白名单）
     */
    private void configureExcludePaths(SaServletFilter filter) {
        // 默认排除路径
        filter.addExclude(
                "/favicon.ico",
                "/test/**"
        );

        // 从配置文件读取的排除路径
        if (!saTokenProperties.getExcludePaths().isEmpty()) {
            filter.addExclude(saTokenProperties.getExcludePaths().toArray(new String[0]));
        }
    }

    /**
     * 配置响应头
     */
    private void configureHeaders() {
        if (saTokenProperties.getAllowCrossOrigin()) {
            SaHolder.getResponse()
                    .setHeader("Access-Control-Allow-Origin", saTokenProperties.getAllowOrigin())
                    .setHeader("Access-Control-Allow-Credentials", "true")
                    .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT")
                    .setHeader("Access-Control-Allow-Headers", "*")
                    .setHeader("Access-Control-Max-Age", "3600");
        }

        // 安全头配置
        SaHolder.getResponse()
                .setServer("sa-server")
                .setHeader("X-Frame-Options", "SAMEORIGIN")
                .setHeader("X-XSS-Protection", "1; mode=block")
                .setHeader("X-Content-Type-Options", "nosniff")
                .setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    /**
     * 处理 OPTIONS 预检请求
     */
    private void handleOptionsRequest() {
        if ("OPTIONS".equals(SaHolder.getRequest().getMethod())) {
            SaHolder.getResponse().setStatus(200);
            SaRouter.back();
        }
    }
}
