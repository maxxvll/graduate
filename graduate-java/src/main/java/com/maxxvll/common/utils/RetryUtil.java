package com.maxxvll.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 异步任务重试工具
 * <p>
 * 提供基于指数退避的重试机制，适用于：
 * - 网络请求失败重试
 * - 数据库操作失败重试
 * - 第三方服务调用失败重试
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 简单重试
 * String result = RetryUtil.execute(() -> callApi(), 3);
 *
 * // 带指数退避重试
 * String result = RetryUtil.executeWithBackoff(
 *     () -> callApi(),
 *     3,           // 最大重试次数
 *     1000,        // 初始延迟(ms)
 *     2.0,         // 退避倍数
 *     10000        // 最大延迟(ms)
 * );
 *
 * // 带异常判断的重试
 * String result = RetryUtil.executeWithRetry(
 *     () -> callApi(),
 *     3,
 *     IOException.class // 只对这些异常重试
 * );
 * }</pre>
 *
 * @author backend
 * @since 2026-03-31
 */
@Slf4j
public class RetryUtil {

    /** 默认最大重试次数 */
    public static final int DEFAULT_MAX_RETRIES = 3;

    /** 默认初始延迟(ms) */
    public static final long DEFAULT_INITIAL_DELAY_MS = 1000;

    /** 默认退避倍数 */
    public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    /** 默认最大延迟(ms) */
    public static final long DEFAULT_MAX_DELAY_MS = 10000;

    private RetryUtil() {}

    /**
     * 执行带重试的操作（简单版，使用默认参数）
     *
     * @param operation 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public static <T> T execute(Supplier<T> operation) throws Exception {
        return executeWithBackoff(operation, DEFAULT_MAX_RETRIES);
    }

    /**
     * 执行带重试的操作
     *
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public static <T> T executeWithBackoff(Supplier<T> operation, int maxRetries) throws Exception {
        return executeWithBackoff(operation, maxRetries, DEFAULT_INITIAL_DELAY_MS);
    }

    /**
     * 执行带指数退避重试的操作
     *
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟(ms)
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public static <T> T executeWithBackoff(
            Supplier<T> operation,
            int maxRetries,
            long initialDelayMs) throws Exception {
        return executeWithBackoff(operation, maxRetries, initialDelayMs, DEFAULT_BACKOFF_MULTIPLIER);
    }

    /**
     * 执行带完全指数退避重试的操作
     *
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟(ms)
     * @param multiplier 退避倍数
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public static <T> T executeWithBackoff(
            Supplier<T> operation,
            int maxRetries,
            long initialDelayMs,
            double multiplier) throws Exception {
        return executeWithBackoff(operation, maxRetries, initialDelayMs, multiplier, DEFAULT_MAX_DELAY_MS);
    }

    /**
     * 执行带完全指数退避重试的操作（带最大延迟限制）
     *
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟(ms)
     * @param multiplier 退避倍数
     * @param maxDelayMs 最大延迟(ms)
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败
     */
    public static <T> T executeWithBackoff(
            Supplier<T> operation,
            int maxRetries,
            long initialDelayMs,
            double multiplier,
            long maxDelayMs) throws Exception {

        Exception lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt > maxRetries) {
                    log.warn("重试次数已用完，最后一次尝试失败: attempt={}, maxRetries={}",
                        attempt, maxRetries);
                    break;
                }

                log.warn("操作失败，准备重试: attempt={}/{}-{}, delay={}ms, error={}",
                    attempt, attempt + 1, maxRetries + 1, currentDelay, e.getMessage());

                sleep(currentDelay);

                // 计算下次延迟（指数退避）
                currentDelay = (long) Math.min(currentDelay * multiplier, maxDelayMs);
            }
        }

        throw lastException;
    }

    /**
     * 执行带特定异常类型判断的重试
     *
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数
     * @param retryableExceptions 要重试的异常类型
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws Exception 如果所有重试都失败，或遇到非重试类型的异常
     */
    @SafeVarargs
    public static <T> T executeWithRetry(
            Supplier<T> operation,
            int maxRetries,
            Class<? extends Exception>... retryableExceptions) throws Exception {

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                // 检查是否为需要重试的异常
                boolean shouldRetry = isRetryableException(e, retryableExceptions);

                if (!shouldRetry || attempt > maxRetries) {
                    if (!shouldRetry) {
                        log.warn("遇到非重试类型异常，不进行重试: exceptionType={}",
                            e.getClass().getSimpleName());
                    }
                    break;
                }

                log.warn("操作失败，准备重试: attempt={}/{}, error={}",
                    attempt, maxRetries + 1, e.getMessage());

                sleep(DEFAULT_INITIAL_DELAY_MS);
            }
        }

        throw lastException;
    }

    /**
     * 执行 Callable 版本的重试（支持抛出检查异常）
     */
    public static <T> T executeCallable(Callable<T> callable) throws Exception {
        return executeWithBackoff(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, DEFAULT_MAX_RETRIES);
    }

    /**
     * 执行 Callable 版本的重试（带参数）
     */
    public static <T> T executeCallable(Callable<T> callable, int maxRetries) throws Exception {
        return executeWithBackoff(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, maxRetries);
    }

    /**
     * 判断是否为需要重试的异常
     */
    @SafeVarargs
    private static boolean isRetryableException(
            Exception e,
            Class<? extends Exception>... retryableExceptions) {
        if (retryableExceptions == null || retryableExceptions.length == 0) {
            return true; // 默认重试所有异常
        }

        for (Class<? extends Exception> retryable : retryableExceptions) {
            if (retryable.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 睡眠（捕获中断异常）
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("重试等待被中断");
        }
    }
}
