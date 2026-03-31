package com.maxxvll.common.annotation;

import com.maxxvll.common.enums.Role;

import java.lang.annotation.*;

/**
 * 权限验证注解
 * 用于标记需要进行权限检查的方法
 *
 * <p>支持以下验证模式：</p>
 * <ul>
 *   <li>权限验证：指定需要的权限列表</li>
 *   <li>角色验证：指定需要的角色</li>
 *   <li>AND/OR 逻辑组合</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 需要指定权限之一（OR逻辑）
 * {@code @RequirePermission("user:read")}
 *
 * // 需要所有指定权限（AND逻辑）
 * {@code @RequirePermission(value = {"user:read", "user:write"}, requireAll = true)}
 *
 * // 需要指定角色
 * {@code @RequirePermission(roles = Role.ADMIN)}
 *
 * // 需要管理员角色且同时拥有指定权限
 * {@code @RequirePermission(roles = Role.ADMIN, permissions = "user:manage")}
 * </pre>
 *
 * @author maxxvll
 * @since 2026-03-16
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限列表
     * 可以指定单个或多个权限
     * 支持权限码格式：user:read, group:create 等
     */
    String[] value() default {};

    /**
     * 需要的角色列表
     * 如果用户拥有列表中的任一角色，即可通过验证
     */
    Role[] roles() default {};

    /**
     * 是否需要拥有所有权限（AND逻辑）
     * true: 需要拥有所有列出的权限
     * false: 只需要拥有其中一个权限即可（OR逻辑）
     */
    boolean requireAll() default false;

    /**
     * 权限验证模式
     *
     * @see PermissionMode
     */
    PermissionMode mode() default PermissionMode.AND_ROLE_PERMISSION;

    /**
     * 资源ID参数名
     * 用于从方法参数中提取资源ID进行细粒度权限验证
     * 例如：userId, groupId
     */
    String resourceParam() default "";

    /**
     * 权限验证失败时的错误消息
     */
    String message() default "权限不足";

    /**
     * 是否记录详细日志
     */
    boolean loggable() default true;

    /**
     * 权限验证模式枚举
     */
    enum PermissionMode {
        /**
         * 角色 AND 权限：需要同时满足角色要求和权限要求
         */
        AND_ROLE_PERMISSION,

        /**
         * 角色 OR 权限：只需要满足角色要求或权限要求之一
         */
        OR_ROLE_PERMISSION,

        /**
         * 仅角色验证：只验证用户角色
         */
        ROLE_ONLY,

        /**
         * 仅权限验证：只验证用户权限
         */
        PERMISSION_ONLY
    }
}
