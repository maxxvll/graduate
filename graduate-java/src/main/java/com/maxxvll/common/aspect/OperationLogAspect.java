package com.maxxvll.common.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.maxxvll.common.annotation.OperationLog;
import com.maxxvll.common.constants.LoggingConstants;
import com.maxxvll.common.logging.MdcHelper;
import com.maxxvll.utils.RedissonCacheUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 操作日志切面
 * <p>
 * 记录被 @OperationLog 注解标记的方法调用日志
 * </p>
 *
 * <p><b>记录内容:</b></p>
 * <ul>
 *     <li>操作用户ID</li>
 *     <li>操作模块和动作</li>
 *     <li>请求参数（可选，敏感参数自动过滤）</li>
 *     <li>响应结果（可选）</li>
 *     <li>执行时间</li>
 *     <li>客户端IP</li>
 *     <li>操作结果（成功/失败）</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final String OP_LOG_PREFIX = "operation_log:";
    private static final int LOG_EXPIRE_DAYS = 30;

    @Resource
    private RedissonCacheUtil redissonCacheUtil;

    /**
     * 环绕通知，记录操作日志
     */
    @Around("@annotation(operationLog)")
    public Object doAround(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String userId = getCurrentUserId();
        String clientIp = getClientIp();
        String requestUri = getRequestUri();
        String httpMethod = getHttpMethod();

        // 构建基础日志信息
        Map<String, Object> logInfo = new HashMap<>();
        logInfo.put("module", operationLog.module());
        logInfo.put("action", operationLog.action());
        logInfo.put("description", operationLog.description());
        logInfo.put("userId", userId);
        logInfo.put("clientIp", clientIp);
        logInfo.put("requestUri", requestUri);
        logInfo.put("httpMethod", httpMethod);
        logInfo.put("method", joinPoint.getSignature().toShortString());
        logInfo.put("startTime", Instant.now().toString());
        logInfo.put("traceId", MdcHelper.getTraceId());

        // 记录请求参数（如果需要）
        if (operationLog.saveParams()) {
            Object[] args = joinPoint.getArgs();
            String[] paramNames = getParamNames(joinPoint);
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length && i < paramNames.length; i++) {
                String paramName = paramNames[i];
                // 过滤敏感参数
                if (!isSensitiveParam(paramName, operationLog.sensitiveParams())) {
                    Object value = args[i];
                    // 过滤null值
                    if (value != null) {
                        try {
                            params.put(paramName, JSON.toJSONString(value));
                        } catch (Exception e) {
                            params.put(paramName, value.toString());
                        }
                    }
                } else {
                    params.put(paramName, "***");
                }
            }
            logInfo.put("params", params);
        }

        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;

        } catch (Throwable e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;

        } finally {
            // 计算执行时间
            long costTime = System.currentTimeMillis() - startTime;
            logInfo.put("costTime", costTime);
            logInfo.put("success", success);
            logInfo.put("timestamp", System.currentTimeMillis());

            // 记录响应结果（如果需要）
            if (operationLog.saveResult() && result != null) {
                try {
                    logInfo.put("result", JSON.toJSONString(result));
                } catch (Exception e) {
                    logInfo.put("result", "序列化失败");
                }
            }

            // 如果操作失败，记录错误信息
            if (!success) {
                logInfo.put("errorMessage", errorMessage);
            }

            // 记录日志
            logOperation(logInfo, operationLog.level(), success, costTime);
        }
    }

    /**
     * 记录操作日志
     */
    private void logOperation(Map<String, Object> logInfo,
                              OperationLog.LogLevel level,
                              boolean success,
                              long costTime) {
        String module = (String) logInfo.get("module");
        String action = (String) logInfo.get("action");
        String userId = (String) logInfo.get("userId");
        String clientIp = (String) logInfo.get("clientIp");
        long startTime = System.currentTimeMillis();

        // 根据日志级别记录
        switch (level) {
            case DEBUG:
                log.debug("[操作日志] {}:{} by user={} from ip={} cost={}ms success={}",
                        module, action, userId, clientIp, costTime, success);
                break;

            case WARN:
                log.warn("[操作日志] {}:{} by user={} from ip={} cost={}ms success={}",
                        module, action, userId, clientIp, costTime, success);
                break;

            case ERROR:
                log.error("[操作日志] {}:{} by user={} from ip={} cost={}ms success={} error={}",
                        module, action, userId, clientIp, costTime, success, logInfo.get("errorMessage"));
                break;

            case INFO:
            default:
                if (success) {
                    log.info("[操作日志] {}:{} by user={} from ip={} cost={}ms",
                            module, action, userId, clientIp, costTime);
                } else {
                    log.warn("[操作日志] {}:{} by user={} from ip={} cost={}ms error={}",
                            module, action, userId, clientIp, costTime, logInfo.get("errorMessage"));
                }
                break;
        }

        // 如果是慢操作（超过5秒），记录警告
        if (costTime > 5000) {
            log.warn("[慢操作] {}:{} cost={}ms 超过5秒阈值",
                    module, action, costTime);
        }
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsString();
            }
        } catch (Exception e) {
            // 忽略
        }
        return "anonymous";
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * 获取请求URI
     */
    private String getRequestUri() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : "unknown";
    }

    /**
     * 获取HTTP方法
     */
    private String getHttpMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : "unknown";
    }

    /**
     * 获取请求参数名称
     */
    private String[] getParamNames(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getParameterNames();
    }

    /**
     * 判断参数是否为敏感参数
     */
    private boolean isSensitiveParam(String paramName, String[] sensitiveParams) {
        if (paramName == null || sensitiveParams == null) {
            return false;
        }
        String lowerParamName = paramName.toLowerCase();
        return Arrays.stream(sensitiveParams)
                .anyMatch(s -> s.toLowerCase().contains(lowerParamName) ||
                               lowerParamName.contains(s.toLowerCase()));
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
