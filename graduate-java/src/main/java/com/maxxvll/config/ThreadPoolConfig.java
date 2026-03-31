package com.maxxvll.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置
 * 用于处理异步业务逻辑，避免阻塞 Netty EventLoop 线程
 *
 * 配置说明：
 * - nettyBusinessExecutor: Netty 业务线程池
 * - kafkaBatchExecutor: Kafka 批处理线程池
 * - fileIoExecutor: 文件 I/O 线程池
 * - asyncExecutor: Spring 异步任务线程池
 *
 * 拒绝策略：
 * - AbortPolicy: 直接抛出异常（默认）
 * - CallerRunsPolicy: 由调用线程执行
 * - DiscardPolicy: 静默丢弃
 * - DiscardOldestPolicy: 丢弃最旧的任务
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig implements AsyncConfigurer {

    @Value("${thread.pool.core-size:#{null}}")
    private Integer customCoreSize;

    @Value("${thread.pool.max-size:#{null}}")
    private Integer customMaxSize;

    /**
     * Netty 业务线程池
     * 用于处理 WebSocket 消息解析、Token 验证等业务逻辑
     *
     * 配置说明：
     * - 核心线程数：CPU 核心数 × 2
     * - 最大线程数：CPU 核心数 × 4
     * - 队列容量：2000
     * - 拒绝策略：AbortPolicy（直接抛出异常）
     */
    @Bean(name = "nettyBusinessExecutor", destroyMethod = "shutdown")
    public ExecutorService nettyBusinessExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = customCoreSize != null ? customCoreSize : Math.max(4, processors * 2);
        int maxPoolSize = customMaxSize != null ? customMaxSize : Math.max(corePoolSize, processors * 4);

        log.info("初始化 Netty 业务线程池：核心线程数={}, 最大线程数={}, 队列容量={}",
                corePoolSize, maxPoolSize, 2000);

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new NamedThreadFactory("netty-business"),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * Kafka 批处理线程池
     * 用于批量处理 Kafka 消息
     */
    @Bean(name = "kafkaBatchExecutor", destroyMethod = "shutdown")
    public ExecutorService kafkaBatchExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, processors);
        int maxPoolSize = Math.max(corePoolSize, processors * 2);

        log.info("初始化 Kafka 批处理线程池：核心线程数={}, 最大线程数={}",
                corePoolSize, maxPoolSize);

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new NamedThreadFactory("kafka-batch"),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 文件处理线程池
     * 用于处理文件上传、下载等 I/O 密集型操作
     */
    @Bean(name = "fileIoExecutor", destroyMethod = "shutdown")
    public ExecutorService fileIoExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, processors);
        int maxPoolSize = Math.max(corePoolSize, processors * 2);

        log.info("初始化文件 I/O 线程池：核心线程数={}, 最大线程数={}",
                corePoolSize, maxPoolSize);

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                120L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new NamedThreadFactory("file-io"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Spring 异步任务线程池
     * 用于 @Async 注解的异步方法
     */
    @Override
    @Bean(name = "asyncExecutor", destroyMethod = "shutdown")
    public Executor getAsyncExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(2, processors);
        int maxPoolSize = Math.max(corePoolSize, processors * 2);

        log.info("初始化 Spring 异步任务线程池：核心线程数={}, 最大线程数={}",
                corePoolSize, maxPoolSize);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> log.error("异步任务执行异常, method={}, params={}",
                method.getName(), params, throwable);
    }

    /**
     * 命名线程工厂
     * 生成有意义的线程名称，便于调试和监控
     */
    public static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final boolean daemon;

        public NamedThreadFactory(String namePrefix) {
            this(namePrefix, false);
        }

        public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(
                    r,
                    namePrefix + "-" + threadNumber.getAndIncrement()
            );
            thread.setDaemon(daemon);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * 线程池监控信息
     * 可用于健康检查和监控端点
     */
    public static class ThreadPoolMonitor {
        private final ExecutorService executor;
        private final String name;

        public ThreadPoolMonitor(ExecutorService executor, String name) {
            this.executor = executor;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public int getActiveCount() {
            if (executor instanceof ThreadPoolExecutor pool) {
                return pool.getActiveCount();
            }
            return -1;
        }

        public int getPoolSize() {
            if (executor instanceof ThreadPoolExecutor pool) {
                return pool.getPoolSize();
            }
            return -1;
        }

        public long getCompletedTaskCount() {
            if (executor instanceof ThreadPoolExecutor pool) {
                return pool.getCompletedTaskCount();
            }
            return -1;
        }

        public int getQueueSize() {
            if (executor instanceof ThreadPoolExecutor pool) {
                return pool.getQueue().size();
            }
            return -1;
        }
    }
}
