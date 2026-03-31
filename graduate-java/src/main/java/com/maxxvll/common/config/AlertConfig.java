package com.maxxvll.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 告警配置
 *
 * <p>配置各种监控指标的告警规则</p>
 *
 * <p>配置示例（application.yaml）：</p>
 * <pre>
 * app:
 *   alert:
 *     enabled: true
 *     rules:
 *       - name: 连接数超限
 *         metric: websocket.connections
 *         threshold: 5000
 *         operator: GREATER_THAN
 *         severity: WARNING
 *         cooldown-seconds: 300
 * </pre>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.alert")
public class AlertConfig {

    /**
     * 是否启用告警
     */
    private boolean enabled = true;

    /**
     * 告警规则列表
     */
    private List<AlertRule> rules = new ArrayList<>();

    /**
     * 告警通知渠道配置
     */
    private List<NotificationChannel> channels = new ArrayList<>();

    /**
     * 告警规则
     */
    @Data
    public static class AlertRule {
        /**
         * 规则名称
         */
        private String name;

        /**
         * 指标名称
         */
        private String metric;

        /**
         * 阈值
         */
        private double threshold;

        /**
         * 比较操作符
         */
        private Operator operator = Operator.GREATER_THAN;

        /**
         * 严重程度：INFO, WARNING, CRITICAL
         */
        private Severity severity = Severity.WARNING;

        /**
         * 冷却时间（秒）- 告警触发后的冷却期
         */
        private int cooldownSeconds = 300;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 通知渠道
     */
    @Data
    public static class NotificationChannel {
        /**
         * 渠道类型：LOG, EMAIL, WEBHOOK
         */
        private String type = "LOG";

        /**
         * 渠道配置
         */
        private String config;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 比较操作符
     */
    public enum Operator {
        GREATER_THAN(">"),
        LESS_THAN("<"),
        EQUALS("="),
        GREATER_THAN_OR_EQUALS(">="),
        LESS_THAN_OR_EQUALS("<=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public boolean evaluate(double value, double threshold) {
            return switch (this) {
                case GREATER_THAN -> value > threshold;
                case LESS_THAN -> value < threshold;
                case EQUALS -> value == threshold;
                case GREATER_THAN_OR_EQUALS -> value >= threshold;
                case LESS_THAN_OR_EQUALS -> value <= threshold;
            };
        }
    }

    /**
     * 严重程度
     */
    public enum Severity {
        INFO("信息"),
        WARNING("警告"),
        CRITICAL("严重");

        private final String desc;

        Severity(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
    }
}
