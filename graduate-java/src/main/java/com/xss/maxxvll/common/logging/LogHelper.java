package com.maxxvll.common.logging;

import com.maxxvll.common.constants.LoggingConstants;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 统一日志工具类
 * <p>
 * 提供统一的日志记录方法，遵循阿里巴巴Java开发手册的日志规范
 * 支持接口日志、业务日志、性能日志、外部服务日志、缓存日志、数据库日志等
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 接口日志
 * LogHelper.logRequestStart("ChatController", "sendMessage", args);
 * LogHelper.logRequestEnd("ChatController", "sendMessage", 150);
 *
 * // 业务日志
 * LogHelper.logBusiness(LoggingConstants.OP_MESSAGE_SEND, messageId, "消息发送成功");
 *
 * // 性能监控
 * LogHelper.logPerformanceWarn("数据库查询", 1200, LoggingConstants.DATABASE_PERFORMANCE_THRESHOLD_MS);
 *
 * // 外部服务调用
 * LogHelper.logExternalService("MinIO", "上传文件", 500, true);
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-16
 */
@Slf4j
public final class LogHelper {

    private LogHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 接口日志 ====================

    /**
     * 记录接口请求开始日志
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       方法参数
     */
    public static void logRequestStart(String className, String methodName, Object[] args) {
        if (log.isInfoEnabled()) {
            String argsStr = formatArgs(args);
            log.info("{} {}.{}() 开始处理, args={}",
                LoggingConstants.PREFIX_REQUEST_START,
                className,
                methodName,
                argsStr);
        }
    }

    /**
     * 记录接口请求结束日志
     *
     * @param className  类名
     * @param methodName 方法名
     * @param costTime   执行耗时（毫秒）
     */
    public static void logRequestEnd(String className, String methodName, long costTime) {
        if (log.isInfoEnabled()) {
            log.info("{} {}.{}() 处理完成, costTime={}ms",
                LoggingConstants.PREFIX_REQUEST_END,
                className,
                methodName,
                costTime);
        }
    }

    /**
     * 记录接口请求异常日志
     *
     * @param className  类名
     * @param methodName 方法名
     * @param exception  异常对象
     */
    public static void logRequestException(String className, String methodName, Throwable exception) {
        log.error("{} {}.{}() 处理异常, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_REQUEST_EXCEPTION,
            className,
            methodName,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);
    }

    // ==================== 业务日志 ====================

    /**
     * 记录业务操作日志
     *
     * @param operation   操作名称（使用 LoggingConstants 中定义的常量）
     * @param businessKey 业务主键
     * @param details     详细信息
     */
    public static void logBusiness(String operation, String businessKey, String details) {
        if (log.isInfoEnabled()) {
            log.info("{} 操作={}, businessKey={}, details={}",
                LoggingConstants.PREFIX_BUSINESS,
                operation,
                businessKey,
                details);
        }
    }

    /**
     * 记录业务操作日志（Long类型业务主键）
     *
     * @param operation   操作名称
     * @param businessKey 业务主键
     * @param details     详细信息
     */
    public static void logBusiness(String operation, Long businessKey, String details) {
        logBusiness(operation, businessKey != null ? String.valueOf(businessKey) : "null", details);
    }

    /**
     * 记录业务异常日志
     *
     * @param operation   操作名称
     * @param businessKey 业务主键
     * @param exception   异常对象
     */
    public static void logBusinessException(String operation, String businessKey, Throwable exception) {
        log.error("{} 操作={}, businessKey={}, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_BUSINESS_EXCEPTION,
            operation,
            businessKey,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);
    }

    /**
     * 记录业务异常日志（Long类型业务主键）
     *
     * @param operation   操作名称
     * @param businessKey 业务主键
     * @param exception   异常对象
     */
    public static void logBusinessException(String operation, Long businessKey, Throwable exception) {
        logBusinessException(operation, businessKey != null ? String.valueOf(businessKey) : "null", exception);
    }

    /**
     * 记录系统异常日志
     *
     * @param component  组件名称
     * @param operation  操作名称
     * @param exception  异常对象
     */
    public static void logSystemException(String component, String operation, Throwable exception) {
        log.error("{} 组件={}, 操作={}, exceptionType={}, exceptionMessage={}, stackTrace={}",
            LoggingConstants.PREFIX_SYSTEM_EXCEPTION,
            component,
            operation,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            getStackTrace(exception));
    }

    // ==================== 性能日志 ====================

    /**
     * 记录性能警告日志
     *
     * @param operation  操作名称
     * @param costTime   执行耗时（毫秒）
     * @param threshold  阈值（毫秒）
     */
    public static void logPerformanceWarn(String operation, long costTime, long threshold) {
        log.warn("{} 操作={}, costTime={}ms, threshold={}ms, 超出阈值={}ms",
            LoggingConstants.PREFIX_PERFORMANCE_WARN,
            operation,
            costTime,
            threshold,
            costTime - threshold);
    }

    /**
     * 性能监控（DEBUG级别）
     *
     * @param operation 操作名称
     * @param costTime  执行耗时（毫秒）
     */
    public static void logPerformanceDebug(String operation, long costTime) {
        if (log.isDebugEnabled()) {
            log.debug("{} 操作={}, costTime={}ms",
                LoggingConstants.PREFIX_PERFORMANCE_MONITOR,
                operation,
                costTime);
        }
    }

    /**
     * 监控方法性能（用于切面）
     *
     * @param joinPoint        切入点
     * @param warnThresholdMs  警告阈值（毫秒）
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    public static Object monitorPerformance(ProceedingJoinPoint joinPoint, long warnThresholdMs) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            // 执行方法
            Object result = joinPoint.proceed();

            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;

            // 记录性能日志
            if (costTime > warnThresholdMs) {
                logPerformanceWarn(className + "." + methodName, costTime, warnThresholdMs);
            } else {
                logPerformanceDebug(className + "." + methodName, costTime);
            }

            return result;
        } catch (Throwable throwable) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("{} 方法={}, 执行失败, costTime={}ms, exception={}",
                LoggingConstants.PREFIX_PERFORMANCE_MONITOR,
                className + "." + methodName,
                costTime,
                throwable.getMessage(),
                throwable);
            throw throwable;
        }
    }

    /**
     * 性能监控（使用默认阈值）
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    public static Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, LoggingConstants.DEFAULT_PERFORMANCE_THRESHOLD_MS);
    }

    // ==================== 外部服务日志 ====================

    /**
     * 记录外部服务调用日志
     *
     * @param service   服务名称（如 "MinIO"、"Kafka"、"Redis"）
     * @param operation 操作名称（如 "上传文件"、"发送消息"）
     * @param costTime  执行耗时（毫秒）
     * @param success   是否成功
     */
    public static void logExternalService(String service, String operation, long costTime, boolean success) {
        if (success) {
            log.info("{} 服务={}, 操作={}, costTime={}ms, result=SUCCESS",
                LoggingConstants.PREFIX_EXTERNAL_SERVICE,
                service,
                operation,
                costTime);

            // 检查是否超时
            long threshold = LoggingConstants.EXTERNAL_SERVICE_THRESHOLD_MS;
            if (costTime > threshold) {
                log.warn("{} 服务={}, 操作={}, costTime={}ms, 超过阈值={}ms",
                    LoggingConstants.PREFIX_PERFORMANCE_WARN,
                    service,
                    operation,
                    costTime,
                    threshold);
            }
        } else {
            log.error("{} 服务={}, 操作={}, costTime={}ms, result=FAILED",
                LoggingConstants.PREFIX_EXTERNAL_SERVICE,
                service,
                operation,
                costTime);
        }
    }

    /**
     * 记录外部服务调用异常日志
     *
     * @param service   服务名称
     * @param operation 操作名称
     * @param exception 异常对象
     */
    public static void logExternalServiceException(String service, String operation, Throwable exception) {
        log.error("{} 服务={}, 操作={}, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_EXTERNAL_SERVICE,
            service,
            operation,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);
    }

    // ==================== 缓存日志 ====================

    /**
     * 记录缓存操作日志
     *
     * @param operation 操作类型（如 "GET"、"PUT"、"DELETE"）
     * @param key       缓存键
     * @param hit       是否命中（仅对GET操作有效）
     */
    public static void logCacheOperation(String operation, String key, boolean hit) {
        if (log.isDebugEnabled()) {
            log.debug("{} 操作={}, key={}, hit={}",
                LoggingConstants.PREFIX_CACHE,
                operation,
                key,
                hit);
        }
    }

    /**
     * 记录缓存操作耗时
     *
     * @param operation 操作类型
     * @param key       缓存键
     * @param costTime  执行耗时（毫秒）
     */
    public static void logCachePerformance(String operation, String key, long costTime) {
        long threshold = LoggingConstants.CACHE_THRESHOLD_MS;
        if (costTime > threshold) {
            log.warn("{} 操作={}, key={}, costTime={}ms, 超过阈值={}ms",
                LoggingConstants.PREFIX_PERFORMANCE_WARN,
                "缓存操作",
                operation + ":" + key,
                costTime,
                threshold);
        }
    }

    // ==================== 数据库日志 ====================

    /**
     * 记录数据库操作日志
     *
     * @param operation 操作类型（如 "SELECT"、"INSERT"、"UPDATE"、"DELETE"）
     * @param table     表名
     * @param costTime  执行耗时（毫秒）
     */
    public static void logDatabaseOperation(String operation, String table, long costTime) {
        if (log.isDebugEnabled()) {
            log.debug("{} 操作={}, table={}, costTime={}ms",
                LoggingConstants.PREFIX_DATABASE,
                operation,
                table,
                costTime);
        }

        // 检查是否超时
        long threshold = LoggingConstants.DATABASE_PERFORMANCE_THRESHOLD_MS;
        if (costTime > threshold) {
            log.warn("{} 操作={}, table={}, costTime={}ms, 超过阈值={}ms",
                LoggingConstants.PREFIX_PERFORMANCE_WARN,
                "数据库操作",
                operation + ":" + table,
                costTime,
                threshold);
        }
    }

    /**
     * 记录数据库操作异常
     *
     * @param operation 操作类型
     * @param table     表名
     * @param exception 异常对象
     */
    public static void logDatabaseException(String operation, String table, Throwable exception) {
        log.error("{} 操作={}, table={}, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_DATABASE,
            operation,
            table,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);
    }

    // ==================== Kafka 日志 ====================

    /**
     * 记录 Kafka 发送日志
     *
     * @param topic     主题名称
     * @param key       消息键
     * @param costTime  发送耗时（毫秒）
     * @param success   是否成功
     */
    public static void logKafkaSend(String topic, String key, long costTime, boolean success) {
        if (success) {
            log.info("{} topic={}, key={}, costTime={}ms, result=SUCCESS",
                LoggingConstants.PREFIX_KAFKA_SEND,
                topic,
                key,
                costTime);

            // 检查是否超时
            long threshold = LoggingConstants.KAFKA_SEND_THRESHOLD_MS;
            if (costTime > threshold) {
                log.warn("{} topic={}, key={}, costTime={}ms, 超过阈值={}ms",
                    LoggingConstants.PREFIX_PERFORMANCE_WARN,
                    "Kafka发送",
                    topic + ":" + key,
                    costTime,
                    threshold);
            }
        } else {
            log.error("{} topic={}, key={}, costTime={}ms, result=FAILED",
                LoggingConstants.PREFIX_KAFKA_SEND,
                topic,
                key,
                costTime);
        }
    }

    /**
     * 记录 Kafka 消费日志
     *
     * @param topic     主题名称
     * @param partition 分区
     * @param offset    偏移量
     * @param costTime  消费耗时（毫秒）
     */
    public static void logKafkaConsume(String topic, int partition, long offset, long costTime) {
        log.info("{} topic={}, partition={}, offset={}, costTime={}ms",
            LoggingConstants.PREFIX_KAFKA_CONSUME,
            topic,
            partition,
            offset,
            costTime);

        // 检查是否超时
        long threshold = LoggingConstants.KAFKA_CONSUME_THRESHOLD_MS;
        if (costTime > threshold) {
            log.warn("{} topic={}, partition={}, offset={}, costTime={}ms, 超过阈值={}ms",
                LoggingConstants.PREFIX_PERFORMANCE_WARN,
                "Kafka消费",
                topic,
                costTime,
                threshold);
        }
    }

    /**
     * 记录 Kafka 消费异常
     *
     * @param topic     主题名称
     * @param partition 分区
     * @param offset    偏移量
     * @param exception 异常对象
     */
    public static void logKafkaConsumeException(String topic, int partition, long offset, Throwable exception) {
        log.error("{} topic={}, partition={}, offset={}, exceptionType={}, exceptionMessage={}",
            LoggingConstants.PREFIX_KAFKA_CONSUME,
            topic,
            partition,
            offset,
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);
    }

    // ==================== 文件上传日志 ====================

    /**
     * 记录文件上传日志
     *
     * @param fileName  文件名
     * @param fileSize  文件大小（字节）
     * @param costTime  上传耗时（毫秒）
     * @param success   是否成功
     */
    public static void logFileUpload(String fileName, long fileSize, long costTime, boolean success) {
        if (success) {
            log.info("{} fileName={}, fileSize={}KB, costTime={}ms, result=SUCCESS",
                LoggingConstants.PREFIX_FILE_UPLOAD,
                fileName,
                fileSize / 1024,
                costTime);

            // 检查是否超时
            long threshold = LoggingConstants.FILE_UPLOAD_THRESHOLD_MS;
            if (costTime > threshold) {
                log.warn("{} fileName={}, costTime={}ms, 超过阈值={}ms",
                    LoggingConstants.PREFIX_PERFORMANCE_WARN,
                    "文件上传",
                    fileName,
                    costTime,
                    threshold);
            }
        } else {
            log.error("{} fileName={}, fileSize={}KB, costTime={}ms, result=FAILED",
                LoggingConstants.PREFIX_FILE_UPLOAD,
                fileName,
                fileSize / 1024,
                costTime);
        }
    }

    // ==================== WebSocket 日志 ====================

    /**
     * 记录 WebSocket 消息日志
     *
     * @param userId    用户ID
     * @param messageType 消息类型
     * @param action    操作类型（SEND、RECEIVE）
     */
    public static void logWebSocketMessage(Long userId, String messageType, String action) {
        if (log.isDebugEnabled()) {
            log.debug("{} userId={}, messageType={}, action={}",
                LoggingConstants.PREFIX_WEBSOCKET,
                userId,
                messageType,
                action);
        }
    }

    /**
     * 记录 WebSocket 连接事件
     *
     * @param userId    用户ID
     * @param action    操作类型（CONNECT、DISCONNECT）
     * @param success   是否成功
     */
    public static void logWebSocketConnection(Long userId, String action, boolean success) {
        if (success) {
            log.info("{} userId={}, action={}, result=SUCCESS",
                LoggingConstants.PREFIX_WEBSOCKET,
                userId,
                action);
        } else {
            log.error("{} userId={}, action={}, result=FAILED",
                LoggingConstants.PREFIX_WEBSOCKET,
                userId,
                action);
        }
    }

    // ==================== 参数校验日志 ====================

    /**
     * 记录参数校验失败日志
     *
     * @param uri       请求URI
     * @param fieldName 字段名
     * @param message   错误消息
     */
    public static void logValidationFailure(String uri, String fieldName, String message) {
        log.warn("{} uri={}, field={}, message={}",
            LoggingConstants.PREFIX_VALIDATION,
            uri,
            fieldName,
            message);
    }

    /**
     * 记录参数校验失败日志（批量）
     *
     * @param uri        请求URI
     * @param errorCount 错误数量
     * @param errors     错误详情
     */
    public static void logValidationFailureBatch(String uri, int errorCount, String errors) {
        log.warn("{} uri={}, errorCount={}, errors={}",
            LoggingConstants.PREFIX_VALIDATION,
            uri,
            errorCount,
            errors);
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化方法参数
     *
     * @param args 方法参数数组
     * @return 格式化后的参数字符串
     */
    private static String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
            .map(arg -> {
                if (arg == null) {
                    return "null";
                }
                // 限制参数长度，避免日志过长
                String str = arg.toString();
                if (str.length() > 200) {
                    return str.substring(0, 200) + "...";
                }
                return str;
            })
            .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 获取异常堆栈信息（用于ERROR日志）
     *
     * @param throwable 异常对象
     * @return 堆栈信息字符串
     */
    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        // 只打印前10行堆栈，避免日志过长
        int maxLines = Math.min(stackTrace.length, 10);
        for (int i = 0; i < maxLines; i++) {
            sb.append("\tat ").append(stackTrace[i]).append("\n");
        }

        if (stackTrace.length > maxLines) {
            sb.append("\t... ").append(stackTrace.length - maxLines).append(" more\n");
        }

        return sb.toString();
    }

    /**
     * 获取方法签名（用于日志）
     *
     * @param joinPoint 切入点
     * @return 方法签名字符串
     */
    public static String getMethodSignature(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName() + "()";
    }

    /**
     * 格式化耗时（带单位）
     *
     * @param millis 毫秒数
     * @return 格式化后的耗时字符串
     */
    public static String formatCostTime(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%dm%ds", minutes, seconds);
        }
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2fKB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2fMB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
