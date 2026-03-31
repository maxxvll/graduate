package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 用于标记需要记录操作日志的方法
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * @OperationLog(module = "用户管理", action = "用户登录")
 * @PostMapping("/login")
 * public Result<String> login(@RequestBody LoginDTO dto) {
 *     // ...
 * }
 *
 * @OperationLog(module = "文件管理", action = "上传文件", saveParams = true)
 * @PostMapping("/upload")
 * public Result<String> upload(@RequestParam MultipartFile file) {
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
public @interface OperationLog {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作名称
     */
    String action() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 是否保存请求参数
     */
    boolean saveParams() default false;

    /**
     * 是否保存响应结果
     */
    boolean saveResult() default false;

    /**
     * 敏感参数名称（这些参数不会被记录）
     */
    String[] sensitiveParams() default {"password", "token", "secret", "key"};

    /**
     * 日志级别
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 日志级别枚举
     */
    enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
