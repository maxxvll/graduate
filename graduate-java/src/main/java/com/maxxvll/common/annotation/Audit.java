package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解
 * <p>
 * 用于标记需要进行审计的敏感操作
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @Audit(auditType = "PERMISSION", action = "分配权限", riskLevel = "HIGH")
 * @PostMapping("/permission/assign")
 * public Result<Void> assignPermission(@RequestBody AssignPermDTO dto) {
 *     // ...
 * }
 * }</pre>
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {

    /**
     * 审计类型
     */
    String auditType() default "USER_MANAGEMENT";

    /**
     * 操作动作
     */
    String action() default "";

    /**
     * 目标类型
     */
    String targetType() default "";

    /**
     * 风险等级
     */
    String riskLevel() default "LOW";

    /**
     * 是否记录请求参数
     */
    boolean saveParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean saveResult() default false;

    /**
     * 敏感参数（不会被记录）
     */
    String[] sensitiveParams() default {"password", "token", "secret", "key"};
}
