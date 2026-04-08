package com.maxxvll.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * XSS 过滤器
 * <p>
 * 过滤所有 HTTP 请求，对请求参数进行 XSS 防护处理
 * </p>
 *
 * <p><b>过滤策略:</b></p>
 * <ul>
 *     <li>使用 XssHttpServletRequestWrapper 包装请求</li>
 *     <li>移除 &lt;script&gt;、&lt;iframe&gt; 等危险标签</li>
 *     <li>移除 javascript: 等伪协议</li>
 *     <li>移除 on* 事件处理器</li>
 *     <li>转义 HTML 特殊字符</li>
 * </ul>
 *
 * <p><b>排除路径:</b></p>
 * <ul>
 *     <li>/error - 错误页面</li>
 *     <li>/actuator/** - 健康检查</li>
 *     <li>/swagger-ui/** - API 文档</li>
 *     <li>/v3/api-docs/** - API 文档</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@WebFilter(urlPatterns = "/*", filterName = "xssFilter")
public class XssFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS Filter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 检查是否需要过滤
        if (shouldFilter(httpRequest)) {
            // 使用 XSS 包装器包装请求
            XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper(httpRequest);
            log.debug("XSS 过滤 applied to: {}", httpRequest.getRequestURI());
            chain.doFilter(xssRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        log.info("XSS Filter 销毁");
    }

    /**
     * 判断请求是否需要过滤
     *
     * @param request HTTP 请求
     * @return true 如果需要过滤
     */
    private boolean shouldFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 排除不需要过滤的路径
        if (uri.startsWith("/error") ||
                uri.startsWith("/actuator") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-resources") ||
                uri.startsWith("/api/cloud/") ||
                uri.startsWith("/cloud/") ||
                uri.startsWith("/webjars") ||
                uri.contains("/static/") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".png") ||
                uri.endsWith(".jpg") ||
                uri.endsWith(".gif") ||
                uri.endsWith(".ico") ||
                uri.endsWith(".svg") ||
                uri.endsWith(".woff") ||
                uri.endsWith(".woff2") ||
                uri.endsWith(".ttf")) {
            return false;
        }

        return true;
    }
}
