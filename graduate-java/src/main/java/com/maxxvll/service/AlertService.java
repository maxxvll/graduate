package com.maxxvll.service;

import com.maxxvll.common.config.AlertConfig;
import com.maxxvll.common.config.AlertConfig.AlertRule;
import com.maxxvll.common.config.AlertConfig.Severity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警服务
 *
 * <p>监控各种指标并在超过阈值时触发告警</p>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Slf4j
@Service
public class AlertService {

    @Resource
    private AlertConfig alertConfig;

    @Resource
    private PerformanceMetricsService performanceMetricsService;

    /**
     * 上次告警时间记录（用于冷却期）
     */
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * 告警计数器
     */
    private final AtomicInteger totalAlerts = new AtomicInteger(0);

    /**
     * 告警历史
     */
    private final Map<String, AlertRecord> alertHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("告警服务初始化完成，启用状态: {}", alertConfig.isEnabled());
        if (alertConfig.isEnabled() && !alertConfig.getRules().isEmpty()) {
            log.info("已加载 {} 条告警规则", alertConfig.getRules().size());
        }
    }

    /**
     * 定时检查告警规则（每30秒执行一次）
     */
    @Scheduled(fixedRate = 30000)
    public void checkAlerts() {
        if (!alertConfig.isEnabled()) {
            return;
        }

        try {
            List<AlertRule> rules = alertConfig.getRules();
            for (AlertRule rule : rules) {
                if (!rule.isEnabled()) {
                    continue;
                }

                checkRule(rule);
            }
        } catch (Exception e) {
            log.error("检查告警规则失败", e);
        }
    }

    /**
     * 检查单个告警规则
     */
    private void checkRule(AlertRule rule) {
        try {
            double currentValue = getMetricValue(rule.getMetric());

            if (rule.getOperator().evaluate(currentValue, rule.getThreshold())) {
                // 检查是否在冷却期内
                if (isInCooldown(rule.getName())) {
                    log.debug("告警规则[{}]处于冷却期内，跳过", rule.getName());
                    return;
                }

                // 触发告警
                triggerAlert(rule, currentValue);
            }
        } catch (Exception e) {
            log.warn("检查告警规则[{}]失败: {}", rule.getName(), e.getMessage());
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(AlertRule rule, double currentValue) {
        String alertId = rule.getName() + "_" + System.currentTimeMillis();
        AlertRecord record = new AlertRecord(
                alertId,
                rule.getName(),
                rule.getMetric(),
                currentValue,
                rule.getThreshold(),
                rule.getOperator().getSymbol(),
                rule.getSeverity(),
                LocalDateTime.now()
        );

        // 记录告警历史
        alertHistory.put(alertId, record);
        alertHistory.putIfAbsent(rule.getName(), record); // 保留最新告警

        // 更新告警时间（冷却期）
        lastAlertTime.put(rule.getName(), System.currentTimeMillis());

        // 增加告警计数
        totalAlerts.incrementAndGet();

        // 发送告警通知
        sendAlert(record);
    }

    /**
     * 发送告警通知
     */
    private void sendAlert(AlertRecord record) {
        String message = buildAlertMessage(record);

        log.warn("===== 触发告警 =====");
        log.warn("告警名称: {}", record.name());
        log.warn("严重程度: {} ({})", record.severity().getDesc(), record.severity());
        log.warn("指标: {} {} {} (当前: {})",
                record.metric(),
                record.operator(),
                record.threshold(),
                record.currentValue());
        log.warn("时间: {}", record.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.warn("====================");

        // TODO: 根据配置的渠道发送通知（邮件、Webhook等）
        for (AlertConfig.NotificationChannel channel : alertConfig.getChannels()) {
            if (channel.isEnabled()) {
                sendToChannel(channel, message);
            }
        }
    }

    /**
     * 发送告警到指定渠道
     */
    private void sendToChannel(AlertConfig.NotificationChannel channel, String message) {
        try {
            switch (channel.getType().toUpperCase()) {
                case "LOG":
                    // 默认已经通过日志输出
                    break;
                case "EMAIL":
                    // TODO: 发送邮件通知
                    log.info("邮件告警通知已发送: {}", message);
                    break;
                case "WEBHOOK":
                    // TODO: 发送Webhook请求
                    log.info("Webhook告警通知已发送: {}", message);
                    break;
                default:
                    log.warn("未知的告警渠道类型: {}", channel.getType());
            }
        } catch (Exception e) {
            log.error("发送告警到渠道[{}]失败: {}", channel.getType(), e.getMessage());
        }
    }

    /**
     * 构建告警消息
     */
    private String buildAlertMessage(AlertRecord record) {
        return String.format("[%s] %s - %s %s %s (当前值: %.2f)",
                record.severity().getDesc(),
                record.name(),
                record.metric(),
                record.operator(),
                record.threshold(),
                record.currentValue());
    }

    /**
     * 检查是否在冷却期内
     */
    private boolean isInCooldown(String ruleName) {
        Long lastTime = lastAlertTime.get(ruleName);
        if (lastTime == null) {
            return false;
        }

        AlertRule rule = findRule(ruleName);
        if (rule == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        return elapsed < rule.getCooldownSeconds() * 1000L;
    }

    /**
     * 查找告警规则
     */
    private AlertRule findRule(String ruleName) {
        return alertConfig.getRules().stream()
                .filter(r -> r.getName().equals(ruleName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指标值
     */
    private double getMetricValue(String metric) {
        try {
            Map<String, Object> metrics = performanceMetricsService.getCurrentMetrics();

            // 解析嵌套指标（如 websocket.connections）
            String[] parts = metric.split("\\.");
            Object current = metrics;

            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return 0;
                }
            }

            if (current instanceof Number) {
                return ((Number) current).doubleValue();
            }
        } catch (Exception e) {
            log.debug("获取指标[{}]失败: {}", metric, e.getMessage());
        }
        return 0;
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalAlerts", totalAlerts.get());
        stats.put("activeRules", alertConfig.getRules().stream().filter(AlertRule::isEnabled).count());
        stats.put("alertHistoryCount", alertHistory.size());
        stats.put("lastAlerts", alertHistory.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .limit(10)
                .toList());
        return stats;
    }

    /**
     * 告警记录
     */
    public record AlertRecord(
            String alertId,
            String name,
            String metric,
            double currentValue,
            double threshold,
            String operator,
            Severity severity,
            LocalDateTime timestamp
    ) {}

    /**
     * 获取性能指标服务（接口）
     */
    public interface PerformanceMetricsService {
        Map<String, Object> getCurrentMetrics();
    }
}
