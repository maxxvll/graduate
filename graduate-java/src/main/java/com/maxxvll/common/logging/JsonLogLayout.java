package com.maxxvll.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON 格式日志布局
 * <p>
 * 用于输出结构化 JSON 日志，便于 ELK Stack 收集和分析
 * </p>
 *
 * <p>输出格式示例：</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-03-31T10:30:15.123+08:00",
 *   "level": "INFO",
 *   "logger": "com.maxxvll.controller.UserController",
 *   "message": "用户登录成功",
 *   "traceId": "abc123",
 *   "userId": "user001",
 *   "thread": "http-nio-5050-exec-1",
 *   "method": "login",
 *   "costTime": 150,
 *   "ip": "192.168.1.100",
 *   "exception": null
 * }
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
public class JsonLogLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper;

    public JsonLogLayout() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> logEntry = new HashMap<>();

        // 时间戳
        logEntry.put("timestamp", DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));

        // 日志级别
        logEntry.put("level", event.getLevel().toString());

        // 日志器名称
        logEntry.put("logger", event.getLoggerName());

        // 消息内容
        logEntry.put("message", event.getFormattedMessage());

        // 线程名
        logEntry.put("thread", event.getThreadName());

        // MDC 上下文信息
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            // 链路追踪 ID
            if (mdc.containsKey("traceId")) {
                logEntry.put("traceId", mdc.get("traceId"));
            }
            // 用户 ID
            if (mdc.containsKey("userId")) {
                logEntry.put("userId", mdc.get("userId"));
            }
            // 业务键
            if (mdc.containsKey("businessKey")) {
                logEntry.put("businessKey", mdc.get("businessKey"));
            }
            // 请求 URI
            if (mdc.containsKey("uri")) {
                logEntry.put("uri", mdc.get("uri"));
            }
            // HTTP 方法
            if (mdc.containsKey("method")) {
                logEntry.put("method", mdc.get("method"));
            }
            // 客户端 IP
            if (mdc.containsKey("ip")) {
                logEntry.put("ip", mdc.get("ip"));
            }
            // 设备类型
            if (mdc.containsKey("deviceType")) {
                logEntry.put("deviceType", mdc.get("deviceType"));
            }
            // 执行时间
            if (mdc.containsKey("costTime")) {
                logEntry.put("costTime", mdc.get("costTime"));
            }
        }

        // 应用名称
        logEntry.put("app", "graduate-im");

        // 异常信息
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            Map<String, Object> exceptionInfo = new HashMap<>();
            exceptionInfo.put("type", throwableProxy.getClassName());
            exceptionInfo.put("message", throwableProxy.getMessage());

            // 堆栈跟踪
            StringBuilder stackTrace = new StringBuilder();
            stackTrace.append(throwableProxy.toString());
            for (StackTraceElementProxy step : throwableProxy.getStackTraceElementProxyArray()) {
                stackTrace.append("\n    at ").append(step.toString());
            }
            exceptionInfo.put("stackTrace", stackTrace.toString());

            logEntry.put("exception", exceptionInfo);
        } else {
            logEntry.put("exception", null);
        }

        // 转换为 JSON 字符串
        try {
            return objectMapper.writeValueAsString(logEntry) + "\n";
        } catch (JsonProcessingException e) {
            // 如果 JSON 序列化失败，返回原始消息
            return "{\"message\":\"" + escapeJson(event.getFormattedMessage()) + "\"}\n";
        }
    }

    /**
     * 转义 JSON 特殊字符
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
