package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 * <p>
 * 使用 Redis 滑动窗口算法实现接口限流，支持：
 * - 按 IP 限流
 * - 按用户限流
 * - 自定义限流 key
 * - 可配置时间窗口和请求次数
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 限制每个用户每分钟最多调用10次
 * @RateLimit(limit = 10, period = 60, limitType = RateLimit.LimitType.USER)
 * @PostMapping("/send-code")
 * public Result<Void> sendCode(@RequestParam String phone) {
 *     // ...
 * }
 *
 * // 限制每个IP每秒最多调用5次
 * @RateLimit(limit = 5, period = 1, limitType = RateLimit.LimitType.IP)
 * @GetMapping("/search")
 * public Result<List<User>> search(@RequestParam String keyword) {
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
public @interface RateLimit {

    /**
     * 限流时间窗口（秒）
     */
    int period() default 60;

    /**
     * 时间窗口内最大请求次数
     */
    int limit() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 自定义限流 key（可选）
     * 当 limitType 为 CUSTOM 时使用
     */
    String key() default "";

    /**
     * 限流提示信息
     */
    String message() default "访问过于频繁，请稍后再试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 默认策略（按 IP + 方法名限流）
         */
        DEFAULT,

        /**
         * 按 IP 限流
         */
        IP,

        /**
         * 按用户限流（需要登录）
         */
        USER,

        /**
         * 自定义 key 限流
         */
        CUSTOM
    }
}
