package com.maxxvll.config;

import com.maxxvll.common.filter.XssFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XSS 过滤器配置
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Configuration
public class XssFilterConfig {

    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(1); // 优先级设置较高
        return registration;
    }
}
