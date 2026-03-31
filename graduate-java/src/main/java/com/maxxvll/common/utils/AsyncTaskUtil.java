package com.maxxvll.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步任务工具类
 * <p>
 * 提供带超时控制的异步任务执行能力：
 * - 支持超时设置
 * - 支持任务取消
 * - 支持超时回调处理
 * - 支持与 @Async 注解结合使用
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 带超时的异步任务
 * CompletableFuture<String> future = AsyncTaskUtil.executeWithTimeout(
 *     () -> {
 *         // 异步任务逻辑
 *         return callApi();
 *     },
 *     5000,  // 5秒超时
 *     TimeUnit.MILLISECONDS
 * );
 *
 * // 执行带超时和超时时回调的任务
 * AsyncTaskUtil.executeWithTimeout(
 *     () -> processData(),
 *     10,
 *     TimeUnit.SECONDS,
 *     () -> log.warn("任务执行超时")
 * );
 *
 * // 使用Spring线程池
 * AsyncTaskUtil.executeWithSpringExecutor(
 *     () -> doSomething(),
 *     "appTaskExecutor"
 * );
 * }</pre>
 *
 * @author backend
 * @since 2026-03-31
 */
@Slf4j
public class AsyncTaskUtil {

    /** Spring应用任务执行器（可选注入） */
    private static ExecutorService springExecutor;

    /**
     * 设置Spring线程池
     * 由Spring在初始化时调用
     */
    public static void setSpringExecutor(ExecutorService executor) {
        springExecutor = executor;
    }

    /** 默认线程池（由 Spring 注入） */
    private static ExecutorService defaultExecutor;

    static {
        // 使用合理的默认线程池配置
        defaultExecutor = new ThreadPoolExecutor(
            2,                      // corePoolSize
            10,                     // maxPoolSize
            60L, TimeUnit.SECONDS,   // keepAliveTime
            new LinkedBlockingQueue<>(100),  // queueCapacity
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "async-task-" + counter.getAndIncrement());
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：由调用线程执行
        );
    }

    private AsyncTaskUtil() {}

    /**
     * 执行带超时的异步任务
     *
     * @param task 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithTimeout(
            Callable<T> task,
            long timeout,
            TimeUnit unit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, defaultExecutor).orTimeout(timeout, unit);
    }

    /**
     * 执行带超时的异步任务（无返回值）
     *
     * @param task 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> executeWithTimeout(
            Runnable task,
            long timeout,
            TimeUnit unit) {
        return CompletableFuture.runAsync(task, defaultExecutor)
            .orTimeout(timeout, unit);
    }

    /**
     * 执行带超时和超时回调的任务
     *
     * @param task 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param timeoutCallback 超时回调
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithTimeout(
            Callable<T> task,
            long timeout,
            TimeUnit unit,
            Runnable timeoutCallback) {

        CompletableFuture<T> future = new CompletableFuture<>();

        defaultExecutor.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        // 超时处理
        defaultExecutor.submit(() -> {
            try {
                Thread.sleep(unit.toMillis(timeout));
                if (!future.isDone()) {
                    future.cancel(false);
                    if (timeoutCallback != null) {
                        timeoutCallback.run();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return future;
    }

    /**
     * 执行带超时和异常回调的任务
     *
     * @param task 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param errorCallback 异常回调
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithTimeoutAndErrorCallback(
            Callable<T> task,
            long timeout,
            TimeUnit unit,
            Consumer<Throwable> errorCallback) {

        CompletableFuture<T> future = executeWithTimeout(task, timeout, unit);

        future.exceptionally(ex -> {
            if (errorCallback != null) {
                errorCallback.accept(ex);
            }
            return null;
        });

        return future;
    }

    /**
     * 执行异步任务（无超时）
     *
     * @param task 任务
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, defaultExecutor);
    }

    /**
     * 执行异步任务（无超时，无返回值）
     *
     * @param task 任务
     * @return CompletableFuture
     */
    public static CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, defaultExecutor);
    }

    /**
     * 使用Spring线程池执行异步任务
     *
     * @param task 任务
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithSpringExecutor(Callable<T> task) {
        ExecutorService executor = springExecutor != null ? springExecutor : defaultExecutor;
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * 使用Spring线程池执行异步任务（带超时）
     *
     * @param task 任务
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithSpringExecutor(
            Callable<T> task, long timeout, TimeUnit unit) {
        ExecutorService executor = springExecutor != null ? springExecutor : defaultExecutor;
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor).orTimeout(timeout, unit);
    }

    /**
     * 执行带重试的异步任务
     *
     * @param task 任务
     * @param maxRetries 最大重试次数
     * @param initialDelay 初始延迟（毫秒）
     * @param <T> 返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            Callable<T> task, int maxRetries, long initialDelay) {
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;
            long currentDelay = initialDelay;

            for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
                try {
                    return task.call();
                } catch (Exception e) {
                    lastException = e;
                    if (attempt > maxRetries) {
                        throw new CompletionException(e);
                    }
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(ie);
                    }
                    currentDelay *= 2; // 简单退避
                }
            }
            throw new CompletionException(lastException);
        }, defaultExecutor);
    }

    /**
     * 获取默认线程池信息
     */
    public static ThreadPoolInfo getThreadPoolInfo() {
        if (defaultExecutor instanceof ThreadPoolExecutor pool) {
            return new ThreadPoolInfo(
                pool.getPoolSize(),
                pool.getActiveCount(),
                pool.getQueue().size(),
                pool.getCompletedTaskCount()
            );
        }
        return new ThreadPoolInfo(-1, -1, -1, -1);
    }

    /**
     * 关闭线程池（应用关闭时调用）
     */
    public static void shutdown() {
        log.info("关闭异步任务线程池...");
        defaultExecutor.shutdown();
        try {
            if (!defaultExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                defaultExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            defaultExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("异步任务线程池已关闭");
    }

    /**
     * 线程池信息
     */
    public record ThreadPoolInfo(
        int poolSize,
        int activeCount,
        int queueSize,
        long completedTaskCount
    ) {}

    /**
     * 异常消费者接口
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }
}
