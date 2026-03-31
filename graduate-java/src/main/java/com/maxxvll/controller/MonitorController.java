package com.maxxvll.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.maxxvll.common.BaseController;
import com.maxxvll.common.Result;
import com.maxxvll.component.NettyChannelManager;
import com.maxxvll.utils.RedissonCacheUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控 Controller
 * 提供系统、JVM、连接、缓存等监控信息
 */
@Slf4j
@RestController
@RequestMapping("/monitor")
@Tag(name = "系统监控", description = "系统监控接口")
public class MonitorController extends BaseController {

    @Resource
    private NettyChannelManager nettyChannelManager;

    @Resource
    private KafkaAdmin kafkaAdmin;

    @Resource
    private RedissonCacheUtil redissonCacheUtil;

    private static final String NETTY_EXECUTOR_BEAN = "nettyBusinessExecutor";

    /**
     * 获取系统信息
     */
    @SaCheckLogin
    @GetMapping("/system")
    @Operation(summary = "获取系统信息", description = "获取系统CPU、内存、运行时长等信息")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // 系统信息
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            info.put("hostname", inetAddress.getHostName());
            info.put("hostAddress", inetAddress.getHostAddress());
            info.put("osName", System.getProperty("os.name"));
            info.put("osVersion", System.getProperty("os.version"));
            info.put("osArch", System.getProperty("os.arch"));
            info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        } catch (UnknownHostException e) {
            log.warn("获取主机信息失败", e);
        }

        // 启动时间
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        info.put("startTime", startTime);
        info.put("uptime", uptime);
        info.put("uptimeStr", formatUptime(uptime));

        // CPU信息
        info.put("systemLoadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());

        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        info.put("totalMemory", runtime.totalMemory());
        info.put("freeMemory", runtime.freeMemory());
        info.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        info.put("maxMemory", runtime.maxMemory());
        info.put("freeMemoryPercent", Math.round((double) runtime.freeMemory() / runtime.totalMemory() * 100));
        info.put("usedMemoryPercent", Math.round((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100));

        return Result.success(info);
    }

    /**
     * 获取JVM信息
     */
    @SaCheckLogin
    @GetMapping("/jvm")
    @Operation(summary = "获取JVM信息", description = "获取JVM堆内存、线程、GC等信息")
    public Result<Map<String, Object>> getJvmInfo() {
        Map<String, Object> info = new HashMap<>();

        // JVM信息
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("javaVmName", System.getProperty("java.vm.name"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("javaSpecVersion", System.getProperty("java.specification.version"));

        // 堆内存
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        info.put("heapInit", heapUsage.getInit());
        info.put("heapUsed", heapUsage.getUsed());
        info.put("heapCommitted", heapUsage.getCommitted());
        info.put("heapMax", heapUsage.getMax());
        info.put("heapUsagePercent", Math.round((double) heapUsage.getUsed() / heapUsage.getMax() * 100));

        // 非堆内存
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        info.put("nonHeapInit", nonHeapUsage.getInit());
        info.put("nonHeapUsed", nonHeapUsage.getUsed());
        info.put("nonHeapCommitted", nonHeapUsage.getCommitted());
        info.put("nonHeapMax", nonHeapUsage.getMax());

        // 线程信息
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        info.put("threadCount", threadMXBean.getThreadCount());
        info.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        info.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        info.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());

        // GC信息
        var gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        var gcInfo = new HashMap<String, Map<String, Object>>();
        for (var gc : gcMxBeans) {
            Map<String, Object> gcDetail = new HashMap<>();
            gcDetail.put("name", gc.getName());
            gcDetail.put("collectionCount", gc.getCollectionCount());
            gcDetail.put("collectionTime", gc.getCollectionTime());
            gcInfo.put(gc.getName(), gcDetail);
        }
        info.put("gcInfo", gcInfo);

        return Result.success(info);
    }

    /**
     * 获取WebSocket连接统计
     */
    @SaCheckLogin
    @GetMapping("/connections")
    @Operation(summary = "获取WebSocket连接统计", description = "获取当前WebSocket连接数、设备分布等信息")
    public Result<Map<String, Object>> getConnections() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Map<String, Object> metrics = nettyChannelManager.getMetrics();

            stats.put("activeConnections", metrics.get("activeConnections"));
            stats.put("totalConnections", metrics.get("totalConnections"));
            stats.put("messagesSent", metrics.get("messagesSent"));
            stats.put("messagesReceived", metrics.get("messagesReceived"));
            stats.put("connectionsPerDevice", metrics.get("connectionsPerDevice"));
            stats.put("connectionsPerIp", metrics.get("connectionsPerIp"));

            // 活跃用户数
            var onlineUsers = nettyChannelManager.getOnlineUsers();
            stats.put("activeUserCount", onlineUsers != null ? onlineUsers.size() : 0);
            stats.put("onlineUsers", onlineUsers);

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取WebSocket连接统计失败", e);
            return Result.fail("获取连接统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取Kafka消费统计
     */
    @SaCheckLogin
    @GetMapping("/kafka")
    @Operation(summary = "获取Kafka消费统计", description = "获取Kafka消费者组、消费lag等信息")
    public Result<Map<String, Object>> getKafkaStats() {
        Map<String, Object> stats = new HashMap<>();

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
            var groupIds = groupsResult.all().get(5, TimeUnit.SECONDS);

            var groupInfos = new HashMap<String, Map<String, Object>>();
            for (var groupId : groupIds) {
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("groupId", groupId.groupId());
                groupInfo.put("state", groupId.state().toString());
                groupInfos.put(groupId.groupId(), groupInfo);
            }
            stats.put("consumerGroups", groupInfos);
            stats.put("totalGroups", groupIds.size());
            stats.put("status", "Kafka连接正常");

        } catch (Exception e) {
            log.error("获取Kafka统计失败", e);
            stats.put("status", "Kafka连接异常");
            stats.put("error", e.getMessage());
        }

        return Result.success(stats);
    }

    /**
     * 获取Redis缓存统计
     */
    @SaCheckLogin
    @GetMapping("/cache")
    @Operation(summary = "获取Redis缓存统计", description = "获取Redis连接数、缓存键数量等信息")
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 获取Redis信息
            Properties info = redissonCacheUtil.getInfo();
            if (info != null) {
                stats.put("redisVersion", info.get("redis_version"));
                stats.put("status", info.get("status"));
                stats.put("client", info.get("client"));
            }

            // 获取Redisson统计信息
            Map<String, Object> redissonStats = redissonCacheUtil.getStats();
            if (redissonStats != null) {
                redissonStats.forEach(stats::put);
            }

        } catch (Exception e) {
            log.error("获取Redis统计失败", e);
            stats.put("status", "Redis连接异常");
            stats.put("error", e.getMessage());
        }

        return Result.success(stats);
    }

    /**
     * 获取应用健康状态汇总
     */
    @SaCheckLogin
    @GetMapping("/health")
    @Operation(summary = "获取应用健康状态汇总", description = "获取各组件健康状态汇总信息")
    public Result<Map<String, Object>> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();

        // WebSocket健康
        try {
            Map<String, Object> wsMetrics = nettyChannelManager.getMetrics();
            Integer activeConnections = (Integer) wsMetrics.get("activeConnections");
            summary.put("websocket", Map.of(
                "status", "UP",
                "activeConnections", activeConnections != null ? activeConnections : 0
            ));
        } catch (Exception e) {
            summary.put("websocket", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // Redis健康
        try {
            Properties info = redissonCacheUtil.getInfo();
            summary.put("redis", Map.of(
                "status", "UP",
                "connected", info != null
            ));
        } catch (Exception e) {
            summary.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // Kafka健康
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var nodes = adminClient.describeCluster().nodes().get(3, TimeUnit.SECONDS);
            summary.put("kafka", Map.of(
                "status", "UP",
                "nodeCount", nodes.size()
            ));
        } catch (Exception e) {
            summary.put("kafka", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        // 内存健康
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double usagePercent = (double) usedMemory / maxMemory * 100;
        String memStatus = usagePercent < 80 ? "UP" : (usagePercent < 90 ? "WARNING" : "DOWN");
        summary.put("memory", Map.of(
            "status", memStatus,
            "usedPercent", Math.round(usagePercent)
        ));

        return Result.success(summary);
    }

    /**
     * 格式化运行时长
     */
    private String formatUptime(long uptimeMs) {
        long days = uptimeMs / (24 * 60 * 60 * 1000);
        long hours = (uptimeMs % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptimeMs % (60 * 60 * 1000)) / (60 * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");

        return sb.length() > 0 ? sb.toString() : "不到1分钟";
    }
}
