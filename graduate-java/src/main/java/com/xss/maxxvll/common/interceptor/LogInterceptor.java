package com.maxxvll.common.interceptor;

import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.logging.MdcHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 日志拦截器
 * <p>
 * 自动管理 MDC 上下文的生命周期，记录请求的开始和结束
 * 为每个请求生成唯一的 TraceId，用于追踪完整的调用链路
 * </p>
 *
 * <p><b>功能:</b></p>
 * <ul>
 *     <li>请求开始时：初始化 MDC 上下文，生成 TraceId</li>
 *     <li>请求结束时：清理 MDC 上下文，记录请求耗时</li>
 *     <li>异常时：记录异常信息</li>
 * </ul>
 *
 * <p><b>拦截顺序:</b></p>
 * <pre>请求 → LogInterceptor.preHandle() → Controller → Service → ... → LogInterceptor.afterCompletion()</pre>
 *
 * @author Claude Code
 * @see com.maxxvll.common.logging.MdcHelper
 * @since 2026-03-16
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    /**
     * 请求开始时间的属性名
     */
    private static final String ATTR_START_TIME = "REQUEST_START_TIME";

    /**
     * 请求开始时调用
     * <p>
     * 初始化 MDC 上下文，生成 TraceId，记录请求信息
     * </p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return true 表示继续执行，false 表示中断
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(ATTR_START_TIME, startTime);

        // 初始化 MDC 上下文
        String traceId = initMdcContext(request);

        // 从请求头中获取上游 TraceId（用于分布式追踪）
        String headerTraceId = request.getHeader("X-TraceId");
        if (headerTraceId != null && !headerTraceId.trim().isEmpty()) {
            MdcHelper.setTraceId(headerTraceId);
            traceId = headerTraceId;
        }

        // 记录请求开始日志
        logRequestStart(request, handler);

        return true;
    }

    /**
     * 请求处理完成后调用（在视图渲染之后）
     * <p>
     * 记录请求结束日志和响应状态
     * </p>
     *
     * @param request      HTTP 请求
     * @param response     HTTP 响应
     * @param handler      处理器
     * @param modelAndView 视图模型
     * @throws Exception 异常
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 记录响应状态（仅在响应状态为非 2xx 时记录）
        int status = response.getStatus();
        if (status >= 400) {
            log.warn("{} 响应异常, uri={}, method={}, status={}",
                LoggingConstants.PREFIX_REQUEST_END,
                request.getRequestURI(),
                request.getMethod(),
                status);
        }
    }

    /**
     * 请求完成后调用（在整个请求结束时）
     * <p>
     * 清理 MDC 上下文，记录请求耗时
     * </p>
     * <p>
     * <b>重要：</b>无论请求成功还是失败，都会执行此方法
     * </p>
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常（如果发生异常）
     * @throws Exception 异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            // 计算请求耗时
            Long startTimeAttr = (Long) request.getAttribute(ATTR_START_TIME);
            long costTime = 0L;
            if (startTimeAttr != null) {
                costTime = System.currentTimeMillis() - startTimeAttr;
            }

            // 记录请求结束日志
            logRequestEnd(request, response, costTime, ex);

            // 如果有异常，记录异常信息
            if (ex != null) {
                logRequestException(request, ex);
            }

            // 性能监控：如果请求耗时超过阈值，记录警告
            long threshold = LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS;
            if (costTime > threshold) {
                log.warn("{} 请求耗时过长, uri={}, method={}, costTime={}ms, threshold={}ms",
                    LoggingConstants.PREFIX_PERFORMANCE_WARN,
                    request.getRequestURI(),
                    request.getMethod(),
                    costTime,
                    threshold);
            }
        } finally {
            // 清理 MDC 上下文（必须在 finally 中执行）
            MdcHelper.clearContext();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 初始化 MDC 上下文
     *
     * @param request HTTP 请求
     * @return 生成的 TraceId
     */
    private String initMdcContext(HttpServletRequest request) {
        // 生成或获取 TraceId
        String traceId = MdcHelper.initContext();

        // 设置请求相关信息
        MdcHelper.setUri(request.getRequestURI());
        MdcHelper.setMethod(request.getMethod());
        MdcHelper.setIp(getClientIp(request));

        return traceId;
    }

    /**
     * 记录请求开始日志
     *
     * @param request HTTP 请求
     * @param handler 处理器
     */
    private void logRequestStart(HttpServletRequest request, Object handler) {
        log.info("{} 开始处理请求, uri={}, method={}, ip={}, handlerClass={}",
            LoggingConstants.PREFIX_REQUEST_START,
            request.getRequestURI(),
            request.getMethod(),
            getClientIp(request),
            getHandlerClassName(handler));
    }

    /**
     * 记录请求结束日志
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param costTime 请求耗时（毫秒）
     * @param ex       异常（如果存在）
     */
    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, long costTime, Exception ex) {
        if (ex == null) {
            log.info("{} 请求处理完成, uri={}, method={}, status={}, costTime={}ms",
                LoggingConstants.PREFIX_REQUEST_END,
                request.getRequestURI(),
                request.getMethod(),
                response.getStatus(),
                costTime);
        }
    }

    /**
     * 记录请求异常日志
     *
     * @param request HTTP 请求
     * @param ex      异常
     */
    private void logRequestException(HttpServletRequest request, Exception ex) {
        log.error("{} 请求处理异常, uri={}, method={}, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_REQUEST_EXCEPTION,
            request.getRequestURI(),
            request.getMethod(),
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            ex);
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 优先从代理头中获取，支持以下代理头：
     * <ul>
     *     <li>X-Forwarded-For</li>
     *     <li>X-Real-IP</li>
     *     <li>Proxy-Client-IP</li>
     *     <li>WL-Proxy-Client-IP</li>
     * </ul>
     * 如果都没有，则使用 request.getRemoteAddr()
     * </p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 检查各种代理头
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // 如果没有代理头，使用远程地址
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 获取处理器类名
     *
     * @param handler 处理器
     * @return 类名
     */
    private String getHandlerClassName(Object handler) {
        if (handler == null) {
            return "unknown";
        }
        String className = handler.getClass().getSimpleName();
        // 如果是 Spring MVC 的 HandlerMethod，提取实际的方法信息
        if (handler instanceof org.springframework.web.method.HandlerMethod) {
            org.springframework.web.method.HandlerMethod handlerMethod =
                (org.springframework.web.method.HandlerMethod) handler;
            return handlerMethod.getBeanType().getSimpleName() + "." +
                   handlerMethod.getMethod().getName() + "()";
        }
        return className;
    }
}
