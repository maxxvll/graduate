package com.maxxvll.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Sa-Token 权限认证] 配置类
 */
@Configuration
public class SaTokenConfigure {
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * 注册 [Sa-Token全局过滤器]
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()

                // 指定 拦截路由 与 放行路由
                .addInclude("/**").addExclude("/favicon.ico")
                .addExclude("/user/login",
                        "/user/register",
                        "/user/check-username",
                        "/user/qrcode",
                        "/user/sendEmailCode",
                        "/test/**") // 把测试接口和发送验证码也加上

                // 认证函数: 每次请求执行
                .setAuth(obj -> {
                    System.out.println("---------- 进入Sa-Token全局认证 -----------");
                    SaRouter.match("/**", () -> StpUtil.checkLogin());
                })

                // 异常处理函数：每次认证函数发生异常时执行此函数
                .setError(e -> {
                    System.out.println("---------- 进入Sa-Token异常处理 -----------");
                    return SaResult.error(e.getMessage());
                })

                .setBeforeAuth(r -> {

                    SaHolder.getResponse()
                            // 允许的前端地址
                            .setHeader("Access-Control-Allow-Origin", "http://localhost:5100")
                            // 允许所有请求头
                            .setHeader("Access-Control-Allow-Headers", "*")
                            // 允许所有请求方法
                            .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT")
                            // 允许携带凭证（Token）
                            .setHeader("Access-Control-Allow-Credentials", "true")
                            // 预检请求缓存 1 小时
                            .setHeader("Access-Control-Max-Age", "3600");

                    if ("OPTIONS".equals(SaHolder.getRequest().getMethod())) {
                        // 设置响应状态码为200，直接返回
                        SaHolder.getResponse().setStatus(200);
                        // 停止后续执行
                        SaRouter.back();
                    }

                    SaHolder.getResponse()
                            .setServer("sa-server")
                            .setHeader("X-Frame-Options", "SAMEORIGIN")
                            .setHeader("X-XSS-Protection", "1; mode=block")
                            .setHeader("X-Content-Type-Options", "nosniff");
                });
    }
}