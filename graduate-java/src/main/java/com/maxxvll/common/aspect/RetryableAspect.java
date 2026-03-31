package com.maxxvll.common.aspect;

import com.maxxvll.common.annotation.Retryable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 可重试注解切面
 * <p>
 * 使用AOP拦截带有 @Retryable 注解的方法
 * 实现指数退避重试策略
 * </p>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Slf4j
@Aspect
@Component
public class RetryableAspect {

    /**
     * 环绕通知：拦截 @Retryable 注解的方法
     */
    @Around("@annotation(com.maxxvll.common.annotation.Retryable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        Retryable retryable = method.getAnnotation(Retryable.class);

        int maxAttempts = retryable.maxAttempts();
        long initialDelay = retryable.initialDelay();
        double multiplier = retryable.multiplier();
        long maxDelay = retryable.maxDelay();
        Class<? extends Throwable>[] retryFor = retryable.retryFor();
        Class<? extends Throwable>[] noRetryFor = retryable.noRetryFor();

        Exception lastException = null;
        long currentDelay = initialDelay;

        for (int attempt = 1; attempt <= maxAttempts + 1; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                // 检查是否应该跳过重试
                if (shouldNotRetry(e, retryFor, noRetryFor)) {
                    log.debug("Exception {} is not retryable, giving up", e.getClass().getSimpleName());
                    throw e;
                }

                // 检查是否还有重试次数
                if (attempt > maxAttempts) {
                    log.warn("Max retry attempts ({}) reached for method {}",
                            maxAttempts, method.getName());
                    throw e;
                }

                log.warn("Retry attempt {}/{} for method {} failed, delay={}ms, error={}",
                        attempt, maxAttempts, method.getName(), currentDelay, e.getMessage());

                // 等待后重试
                Thread.sleep(currentDelay);

                // 计算下次延迟（指数退避）
                currentDelay = (long) Math.min(currentDelay * multiplier, maxDelay);
            }
        }

        throw lastException;
    }

    /**
     * 判断是否不应该重试
     */
    private boolean shouldNotRetry(Exception e,
                                   Class<? extends Throwable>[] retryFor,
                                   Class<? extends Throwable>[] noRetryFor) {
        // 检查是否在不需要重试列表中
        for (Class<? extends Throwable> noRetryClass : noRetryFor) {
            if (noRetryClass.isInstance(e)) {
                return true;
            }
        }

        // 如果指定了需要重试的异常类型，检查是否匹配
        if (retryFor.length > 0) {
            for (Class<? extends Throwable> retryClass : retryFor) {
                if (retryClass.isInstance(e)) {
                    return false;
                }
            }
            return true; // 不在列表中，不重试
        }

        return false;
    }

    /**
     * 获取被拦截的方法
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }
}
