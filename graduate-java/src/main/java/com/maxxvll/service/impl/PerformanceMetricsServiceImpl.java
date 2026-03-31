package com.maxxvll.service.impl;

import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.service.AlertService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能指标服务实现
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Service
public class PerformanceMetricsServiceImpl implements AlertService.PerformanceMetricsService {

    @Resource
    private NettyChannelManager nettyChannelManager;


    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    // 消息延迟分布统计
    private final LongAdder latencyUnder10ms = new LongAdder();
    private final LongAdder latency10To50ms = new LongAdder();
    private final LongAdder latency50To100ms = new LongAdder();
    private final LongAdder latency100To500ms = new LongAdder();
    private final LongAdder latencyOver500ms = new LongAdder();
    private final AtomicLong totalLatencySamples = new AtomicLong(0);
    private final AtomicLong totalLatencySum = new AtomicLong(0);

    /**
     * 记录消息延迟
     */
    public void recordLatency(long latencyMs) {
        totalLatencySamples.incrementAndGet();
        totalLatencySum.addAndGet(latencyMs);

        if (latencyMs < 10) {
            latencyUnder10ms.increment();
        } else if (latencyMs < 50) {
            latency10To50ms.increment();
        } else if (latencyMs < 100) {
            latency50To100ms.increment();
        } else if (latencyMs < 500) {
            latency100To500ms.increment();
        } else {
            latencyOver500ms.increment();
        }
    }

    @Override
    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();

        // WebSocket 连接指标
        try {
            Map<String, Object> wsMetrics = nettyChannelManager.getMetrics();
            metrics.put("websocket.activeConnections", wsMetrics.getOrDefault("activeConnections", 0));
            metrics.put("websocket.totalConnections", wsMetrics.getOrDefault("totalConnections", 0L));
            metrics.put("websocket.messagesSent", wsMetrics.getOrDefault("messagesSent", 0L));
            metrics.put("websocket.messagesReceived", wsMetrics.getOrDefault("messagesReceived", 0L));
        } catch (Exception e) {
            log.warn("获取WebSocket指标失败", e);
        }

        // JVM 内存指标
        long heapUsed = memoryMXBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        metrics.put("jvm.heapUsed", heapUsed);
        metrics.put("jvm.heapMax", heapMax);
        metrics.put("jvm.heapUsagePercent", heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0);

        // 线程指标
        metrics.put("jvm.threadCount", threadMXBean.getThreadCount());
        metrics.put("jvm.peakThreadCount", threadMXBean.getPeakThreadCount());

        // 延迟分布
        metrics.put("latency.under10ms", latencyUnder10ms.sum());
        metrics.put("latency.10to50ms", latency10To50ms.sum());
        metrics.put("latency.50to100ms", latency50To100ms.sum());
        metrics.put("latency.100to500ms", latency100To500ms.sum());
        metrics.put("latency.over500ms", latencyOver500ms.sum());
        metrics.put("latency.samples", totalLatencySamples.get());

        long samples = totalLatencySamples.get();
        if (samples > 0) {
            metrics.put("latency.avgMs", (double) totalLatencySum.get() / samples);
        }

        return metrics;
    }

    /**
     * 获取延迟分布统计
     */
    public Map<String, Object> getLatencyDistribution() {
        Map<String, Object> distribution = new HashMap<>();

        long samples = totalLatencySamples.get();
        distribution.put("totalSamples", samples);

        if (samples > 0) {
            long under10ms = latencyUnder10ms.sum();
            long under50ms = latency10To50ms.sum() + under10ms;
            long under100ms = latency50To100ms.sum() + under50ms;
            long under500ms = latency100To500ms.sum() + under100ms;

            distribution.put("under10ms", under10ms);
            distribution.put("under10msPercent", (double) under10ms / samples * 100);

            distribution.put("under50ms", under50ms);
            distribution.put("under50msPercent", (double) under50ms / samples * 100);

            distribution.put("under100ms", under100ms);
            distribution.put("under100msPercent", (double) under100ms / samples * 100);

            distribution.put("under500ms", under500ms);
            distribution.put("under500msPercent", (double) under500ms / samples * 100);

            distribution.put("over500ms", latencyOver500ms.sum());
            distribution.put("over500msPercent", (double) latencyOver500ms.sum() / samples * 100);

            distribution.put("avgLatencyMs", (double) totalLatencySum.get() / samples);
        }

        return distribution;
    }

    /**
     * 重置延迟统计
     */
    public void resetLatencyStats() {
        latencyUnder10ms.reset();
        latency10To50ms.reset();
        latency50To100ms.reset();
        latency100To500ms.reset();
        latencyOver500ms.reset();
        totalLatencySamples.set(0);
        totalLatencySum.set(0);
    }
}
