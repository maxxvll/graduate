package com.maxxvll.controller;

import com.maxxvll.common.Result;
import com.maxxvll.config.ThreadPoolConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 线程池监控端点
 * 提供线程池状态查询接口
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
public class ThreadPoolMonitorController {

    @Resource(name = "nettyBusinessExecutor")
    private ExecutorService nettyBusinessExecutor;

    @Resource(name = "kafkaBatchExecutor")
    private ExecutorService kafkaBatchExecutor;

    @Resource(name = "fileIoExecutor")
    private ExecutorService fileIoExecutor;

    @GetMapping("/thread-pools")
    public Result<Map<String, Object>> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("nettyBusiness", getExecutorInfo("nettyBusiness", nettyBusinessExecutor));
        status.put("kafkaBatch", getExecutorInfo("kafkaBatch", kafkaBatchExecutor));
        status.put("fileIo", getExecutorInfo("fileIo", fileIoExecutor));

        return Result.success(status);
    }

    private Map<String, Object> getExecutorInfo(String name, ExecutorService executor) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("type", executor.getClass().getSimpleName());

        if (executor instanceof java.util.concurrent.ThreadPoolExecutor pool) {
            info.put("corePoolSize", pool.getCorePoolSize());
            info.put("maxPoolSize", pool.getMaximumPoolSize());
            info.put("activeCount", pool.getActiveCount());
            info.put("poolSize", pool.getPoolSize());
            info.put("queueSize", pool.getQueue().size());
            info.put("completedTaskCount", pool.getCompletedTaskCount());
            info.put("isShutdown", pool.isShutdown());
            info.put("isTerminated", pool.isTerminated());
        }

        return info;
    }
}
