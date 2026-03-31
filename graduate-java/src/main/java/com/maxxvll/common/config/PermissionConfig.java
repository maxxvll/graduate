package com.maxxvll.common.config;

import com.maxxvll.common.enums.Permission;
import com.maxxvll.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口权限配置
 *
 * <p>支持在配置文件中定义接口的权限要求</p>
 *
 * <p>配置示例（application.yaml）：</p>
 * <pre>
 * app:
 *   permission:
 *     enabled: true
 *     cache-enabled: true
 *     cache-ttl-seconds: 300
 *     rules:
 *       /user/delete:
 *         roles: [ADMIN, SUPER_ADMIN]
 *         permissions: [user:delete]
 *         mode: AND_ROLE_PERMISSION
 * </pre>
 *
 * @author maxxvll
 * @since 2026-03-31
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.permission")
public class PermissionConfig {

    /**
     * 是否启用权限验证
     */
    private boolean enabled = true;

    /**
     * 是否启用权限缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 权限缓存过期时间（秒）
     */
    private long cacheTtlSeconds = 300;

    /**
     * 接口权限规则映射
     * Key: 接口路径（如 /v1/user/delete）
     * Value: 权限规则
     */
    private Map<String, PermissionRule> rules = new ConcurrentHashMap<>();

    /**
     * 初始化后处理
     */
    @PostConstruct
    public void init() {
        // 设置默认值规则
        setDefaultRules();
    }

    /**
     * 设置默认规则
     */
    private void setDefaultRules() {
        // 系统管理接口 - 只能管理员访问
        addRule("/user/delete", PermissionRule.builder()
                .roles(List.of(Role.ADMIN, Role.SUPER_ADMIN))
                .mode(PermissionRule.RuleMode.ROLE_ONLY)
                .build());

        addRule("/user/ban", PermissionRule.builder()
                .roles(List.of(Role.ADMIN, Role.SUPER_ADMIN))
                .mode(PermissionRule.RuleMode.ROLE_ONLY)
                .build());

        addRule("/system/config", PermissionRule.builder()
                .roles(List.of(Role.SUPER_ADMIN))
                .mode(PermissionRule.RuleMode.ROLE_ONLY)
                .build());

        addRule("/system/logs", PermissionRule.builder()
                .roles(List.of(Role.ADMIN, Role.SUPER_ADMIN))
                .permissions(List.of(Permission.SYSTEM_LOG.getCode()))
                .mode(PermissionRule.RuleMode.AND_ROLE_PERMISSION)
                .build());
    }

    /**
     * 添加权限规则
     */
    public void addRule(String path, PermissionRule rule) {
        rules.put(path, rule);
    }

    /**
     * 获取接口的权限规则
     */
    public PermissionRule getRule(String path) {
        // 精确匹配
        PermissionRule rule = rules.get(path);
        if (rule != null) {
            return rule;
        }

        // 前缀匹配（支持通配符）
        for (Map.Entry<String, PermissionRule> entry : rules.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("*") && path.startsWith(pattern.substring(0, pattern.length() - 1))) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 清除所有规则
     */
    public void clearRules() {
        rules.clear();
    }

    /**
     * 权限规则定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionRule {
        /**
         * 需要的角色列表
         */
        @Builder.Default
        private List<Role> roles = new ArrayList<>();

        /**
         * 需要的权限列表（权限码）
         */
        @Builder.Default
        private List<String> permissions = new ArrayList<>();

        /**
         * 验证模式
         */
        @Builder.Default
        private RuleMode mode = RuleMode.AND_ROLE_PERMISSION;

        /**
         * 规则模式枚举
         */
        public enum RuleMode {
            /**
             * 角色 AND 权限
             */
            AND_ROLE_PERMISSION,

            /**
             * 角色 OR 权限
             */
            OR_ROLE_PERMISSION,

            /**
             * 仅角色
             */
            ROLE_ONLY,

            /**
             * 仅权限
             */
            PERMISSION_ONLY
        }

        /**
         * 是否为仅角色模式
         */
        public boolean isRoleOnly() {
            return mode == RuleMode.ROLE_ONLY;
        }

        /**
         * 是否为仅权限模式
         */
        public boolean isPermissionOnly() {
            return mode == RuleMode.PERMISSION_ONLY;
        }

        /**
         * 是否为 AND 模式
         */
        public boolean isAndMode() {
            return mode == RuleMode.AND_ROLE_PERMISSION;
        }
    }
}
