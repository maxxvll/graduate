package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 可重试注解
 * <p>
 * 用于标记需要重试的方法，支持指数退避策略
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Retryable(maxAttempts = 3, initialDelay = 1000, multiplier = 2.0, maxDelay = 10000)
 * public void callExternalService() {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @author backend-msg
 * @since 2026-03-31
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retryable {

    /**
     * 最大重试次数（不含首次执行）
     */
    int maxAttempts() default 3;

    /**
     * 初始延迟时间（毫秒）
     */
    long initialDelay() default 1000;

    /**
     * 退避倍数
     */
    double multiplier() default 2.0;

    /**
     * 最大延迟时间（毫秒）
     */
    long maxDelay() default 10000;

    /**
     * 需要重试的异常类型
     * 如果为空，则重试所有异常
     */
    Class<? extends Throwable>[] retryFor() default {};

    /**
     * 不需要重试的异常类型
     */
    Class<? extends Throwable>[] noRetryFor() default {};
}
