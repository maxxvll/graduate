package com.maxxvll.common.aspect;

import com.maxxvll.common.annotation.PerformanceMonitor;
import com.maxxvll.common.logging.LogHelper;
import com.maxxvll.common.logging.MdcHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 性能监控切面
 * <p>
 * 拦截标记了 @PerformanceMonitor 注解的方法，自动记录执行时间
 * 支持自定义阈值、操作描述、参数记录等配置
 * </p>
 *
 * <p><b>切面优先级:</b></p>
 * <pre>
 * Order(1) - 性能监控切面（最外层）
 *   → 业务逻辑
 *   → Order(2) - 其他切面（如事务切面）
 * </pre>
 * <p>
 * 设置为 Order(1) 确保性能监控包含整个方法的执行时间（包括其他切面的开销）
 * </p>
 *
 * @author Claude Code
 * @see com.maxxvll.common.annotation.PerformanceMonitor
 * @since 2026-03-16
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class PerformanceMonitorAspect {

    /**
     * 环绕通知：拦截所有标记了 @PerformanceMonitor 注解的方法
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(com.maxxvll.common.annotation.PerformanceMonitor)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PerformanceMonitor annotation = method.getAnnotation(PerformanceMonitor.class);

        // 获取注解配置
        long warnThresholdMs = annotation.warnThresholdMs();
        boolean logDebug = annotation.logDebug();
        String description = annotation.description();
        boolean logArgs = annotation.logArgs();
        boolean logResult = annotation.logResult();
        boolean logExceptionStackTrace = annotation.logExceptionStackTrace();

        // 获取操作描述
        String operation = description.isEmpty()
            ? getOperationDescription(joinPoint)
            : description;

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 记录方法调用（如果需要）
            if (logDebug && log.isDebugEnabled()) {
                log.debug("[性能监控] 开始执行: {}{}", operation, formatArgs(logArgs, joinPoint.getArgs()));
            }

            // 执行方法
            Object result = joinPoint.proceed();

            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;

            // 记录成功日志
            if (costTime > warnThresholdMs) {
                // 超过阈值，记录 WARN 日志
                log.warn("[性能警告] 操作={}, costTime={}ms, threshold={}ms, 超出={}ms{}",
                    operation,
                    costTime,
                    warnThresholdMs,
                    costTime - warnThresholdMs,
                    formatResult(logResult, result));
            } else if (logDebug && log.isDebugEnabled()) {
                // 未超过阈值，记录 DEBUG 日志
                log.debug("[性能监控] 操作={}, costTime={}ms{}",
                    operation,
                    costTime,
                    formatResult(logResult, result));
            }

            return result;

        } catch (Throwable throwable) {
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;

            // 记录异常日志
            if (logExceptionStackTrace) {
                // 记录完整异常堆栈
                log.error("[性能监控] 操作={}, 执行异常, costTime={}ms, exceptionType={}, exceptionMessage={}",
                    operation,
                    costTime,
                    throwable.getClass().getSimpleName(),
                    throwable.getMessage(),
                    throwable);
            } else {
                // 只记录异常类型和消息，不记录堆栈
                log.error("[性能监控] 操作={}, 执行异常, costTime={}ms, exceptionType={}, exceptionMessage={}",
                    operation,
                    costTime,
                    throwable.getClass().getSimpleName(),
                    throwable.getMessage());
            }

            // 重新抛出异常
            throw throwable;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 获取操作描述
     *
     * @param joinPoint 切入点
     * @return 操作描述
     */
    private String getOperationDescription(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName + "()";
    }

    /**
     * 格式化方法参数
     *
     * @param logArgs 是否记录参数
     * @param args    方法参数
     * @return 格式化后的参数字符串
     */
    private String formatArgs(boolean logArgs, Object[] args) {
        if (!logArgs || args == null || args.length == 0) {
            return "";
        }

        String argsStr = Arrays.stream(args)
            .map(arg -> {
                if (arg == null) {
                    return "null";
                }
                // 限制参数长度，避免日志过长
                String str = arg.toString();
                if (str.length() > 100) {
                    return str.substring(0, 100) + "...";
                }
                return str;
            })
            .collect(Collectors.joining(", ", ", args=[", "]"));

        return argsStr;
    }

    /**
     * 格式化返回值
     *
     * @param logResult 是否记录返回值
     * @param result    返回值
     * @return 格式化后的返回值字符串
     */
    private String formatResult(boolean logResult, Object result) {
        if (!logResult) {
            return "";
        }

        String resultStr;
        if (result == null) {
            resultStr = "null";
        } else {
            // 限制返回值长度
            String str = result.toString();
            if (str.length() > 200) {
                resultStr = str.substring(0, 200) + "...";
            } else {
                resultStr = str;
            }
        }

        return ", result=" + resultStr;
    }
}
