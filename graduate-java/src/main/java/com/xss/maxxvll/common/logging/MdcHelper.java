package com.maxxvll.common.logging;

import org.slf4j.MDC;
import com.maxxvll.common.constants.LoggingConstants;

import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) 工具类
 * <p>
 * 用于管理日志上下文信息，包括 TraceId、UserId、BusinessKey 等
 * 基于 SLF4J 的 MDC 机制实现
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 在请求开始时初始化上下文
 * MdcHelper.initContext();
 * MdcHelper.setUserId(userId);
 * MdcHelper.setBusinessKey(messageId);
 *
 * // 在业务代码中获取上下文信息
 * String traceId = MdcHelper.getTraceId();
 *
 * // 在请求结束时清理上下文
 * MdcHelper.clearContext();
 * }</pre>
 *
 * @author Claude Code
 * @see org.slf4j.MDC
 * @since 2026-03-16
 */
public final class MdcHelper {

    private MdcHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 上下文初始化和清理 ====================

    /**
     * 初始化 MDC 上下文
     * <p>
     * 生成新的 TraceId 并放入 MDC
     * 通常在请求开始时调用（如拦截器、过滤器）
     * </p>
     *
     * @return 生成的 TraceId
     */
    public static String initContext() {
        String traceId = generateTraceId();
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 初始化 MDC 上下文（使用指定的 TraceId）
     * <p>
     * 用于分布式追踪场景，从上游服务传递 TraceId
     * </p>
     *
     * @param traceId 指定的 TraceId（如果为 null，则自动生成）
     * @return 使用的 TraceId
     */
    public static String initContext(String traceId) {
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
        }
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 清理 MDC 上下文
     * <p>
     * 移除所有 MDC 键值对
     * 通常在请求结束时调用（如拦截器的 afterCompletion）
     * </p>
     * <p>
     * <b>重要：</b>必须在请求结束时调用，否则可能导致内存泄漏
     * </p>
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * 移除指定的 MDC 键
     *
     * @param key MDC 键名
     */
    public static void remove(String key) {
        MDC.remove(key);
    }

    // ==================== TraceId 管理 ====================

    /**
     * 设置 TraceId
     *
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_TRACE_ID, traceId);
        }
    }

    /**
     * 获取当前 TraceId
     *
     * @return TraceId，如果不存在则返回空字符串
     */
    public static String getTraceId() {
        return MDC.get(LoggingConstants.MDC_TRACE_ID);
    }

    /**
     * 生成新的 TraceId
     * <p>
     * 使用 UUID 生成器，去除横线后的32位字符串
     * </p>
     *
     * @return 32位 TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ==================== UserId 管理 ====================

    /**
     * 设置用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(LoggingConstants.MDC_USER_ID, String.valueOf(userId));
        }
    }

    /**
     * 设置用户ID（字符串类型）
     *
     * @param userId 用户ID字符串
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_USER_ID, userId);
        }
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果不存在则返回空字符串
     */
    public static String getUserId() {
        return MDC.get(LoggingConstants.MDC_USER_ID);
    }

    /**
     * 获取当前用户ID（Long类型）
     *
     * @return 用户ID，如果不存在或转换失败则返回 null
     */
    public static Long getUserIdAsLong() {
        String userIdStr = getUserId();
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== BusinessKey 管理 ====================

    /**
     * 设置业务主键
     * <p>
     * 用于标识具体的业务操作对象，如消息ID、订单ID等
     * </p>
     *
     * @param businessKey 业务主键
     */
    public static void setBusinessKey(Long businessKey) {
        if (businessKey != null) {
            MDC.put(LoggingConstants.MDC_BUSINESS_KEY, String.valueOf(businessKey));
        }
    }

    /**
     * 设置业务主键（字符串类型）
     *
     * @param businessKey 业务主键字符串
     */
    public static void setBusinessKey(String businessKey) {
        if (businessKey != null && !businessKey.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_BUSINESS_KEY, businessKey);
        }
    }

    /**
     * 获取当前业务主键
     *
     * @return 业务主键，如果不存在则返回空字符串
     */
    public static String getBusinessKey() {
        return MDC.get(LoggingConstants.MDC_BUSINESS_KEY);
    }

    // ==================== 其他 MDC 属性管理 ====================

    /**
     * 设置请求URI
     *
     * @param uri 请求URI
     */
    public static void setUri(String uri) {
        if (uri != null && !uri.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_URI, uri);
        }
    }

    /**
     * 获取请求URI
     *
     * @return 请求URI
     */
    public static String getUri() {
        return MDC.get(LoggingConstants.MDC_URI);
    }

    /**
     * 设置HTTP方法
     *
     * @param method HTTP方法（GET、POST、PUT、DELETE等）
     */
    public static void setMethod(String method) {
        if (method != null && !method.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_METHOD, method);
        }
    }

    /**
     * 获取HTTP方法
     *
     * @return HTTP方法
     */
    public static String getMethod() {
        return MDC.get(LoggingConstants.MDC_METHOD);
    }

    /**
     * 设置客户端IP地址
     *
     * @param ip IP地址
     */
    public static void setIp(String ip) {
        if (ip != null && !ip.trim().isEmpty()) {
            MDC.put(LoggingConstants.MDC_IP, ip);
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @return IP地址
     */
    public static String getIp() {
        return MDC.get(LoggingConstants.MDC_IP);
    }

    // ==================== 工具方法 ====================

    /**
     * 设置多个 MDC 属性
     *
     * @param keyValuePairs 键值对数组（必须为偶数个元素）
     */
    public static void putMultiple(String... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("键值对数组长度必须为偶数");
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1];
            if (key != null && !key.trim().isEmpty() && value != null) {
                MDC.put(key, value);
            }
        }
    }

    /**
     * 检查 MDC 上下文是否包含指定的键
     *
     * @param key MDC 键名
     * @return 如果包含则返回 true
     */
    public static boolean containsKey(String key) {
        return MDC.get(key) != null;
    }

    /**
     * 获取 MDC 上下文的快照（用于异步任务传递上下文）
     *
     * @return MDC 上下文快照
     */
    public static java.util.Map<String, String> getSnapshot() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * 从快照恢复 MDC 上下文（用于异步任务）
     *
     * @param contextMap MDC 上下文快照
     */
    public static void setSnapshot(java.util.Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(contextMap);
        }
    }

    // ==================== 常用场景方法 ====================

    /**
     * 为异步任务设置 MDC 上下文
     * <p>
     * 在提交异步任务前调用，将当前线程的 MDC 上下文传递给异步线程
     * </p>
     *
     * <pre>{@code
     * // 在主线程中
     * Map<String, String> contextSnapshot = MdcHelper.getAsyncContext();
     * executor.submit(() -> {
     *     try {
     *         MdcHelper.setAsyncContext(contextSnapshot);
     *         // 异步任务代码
     *     } finally {
     *         MdcHelper.clearContext();
     *     }
     * });
     * }</pre>
     *
     * @return MDC 上下文快照
     */
    public static java.util.Map<String, String> getAsyncContext() {
        return getSnapshot();
    }

    /**
     * 为异步任务恢复 MDC 上下文
     *
     * @param contextMap MDC 上下文快照
     */
    public static void setAsyncContext(java.util.Map<String, String> contextMap) {
        setSnapshot(contextMap);
    }

    /**
     * 获取格式化的上下文信息（用于日志输出）
     *
     * @return 格式化的上下文字符串
     */
    public static String getContextInfo() {
        return String.format("traceId=%s, userId=%s, businessKey=%s",
            getTraceId(), getUserId(), getBusinessKey());
    }

    /**
     * 获取调试用的上下文信息（包含更多字段）
     *
     * @return 格式化的调试信息
     */
    public static String getDebugInfo() {
        return String.format(
            "traceId=%s, userId=%s, businessKey=%s, uri=%s, method=%s, ip=%s",
            getTraceId(), getUserId(), getBusinessKey(),
            getUri(), getMethod(), getIp()
        );
    }
}
