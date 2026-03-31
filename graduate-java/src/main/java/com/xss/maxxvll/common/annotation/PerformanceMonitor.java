package com.maxxvll.common.annotation;

import java.lang.annotation.*;

/**
 * 性能监控注解
 * <p>
 * 标记在需要性能监控的方法上，自动记录方法执行时间
 * 当方法执行时间超过指定阈值时，记录 WARN 日志
 * </p>
 *
 * <p><b>使用示例:</b></p>
 * <pre>{@code
 * // 使用默认阈值（500ms）
 * @PerformanceMonitor
 * public void sendMessage(Message message) {
 *     // 业务逻辑
 * }
 *
 * // 自定义阈值
 * @PerformanceMonitor(warnThresholdMs = 3000)
 * public void uploadFile(File file) {
 *     // 业务逻辑
 * }
 *
 * // 禁用DEBUG日志（只记录超时的WARN日志）
 * @PerformanceMonitor(logDebug = false)
 * public void quickOperation() {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * <p><b>注意事项:</b></p>
 * <ul>
 *     <li>基于 Spring AOP 实现，只对 public 方法有效</li>
 *     <li>不支持同一类内部方法调用（绕过代理）</li>
 *     <li>性能监控本身的开销很小（约 1-2ms）</li>
 * </ul>
 *
 * @author Claude Code
 * @see com.maxxvll.common.aspect.PerformanceMonitorAspect
 * @since 2026-03-16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PerformanceMonitor {

    /**
     * 警告阈值（毫秒）
     * <p>
     * 默认值：500ms
     * 当方法执行时间超过此阈值时，记录 WARN 日志
     * </p>
     *
     * @return 阈值（毫秒）
     */
    long warnThresholdMs() default 500L;

    /**
     * 是否记录 DEBUG 日志
     * <p>
     * 默认值：true
     * 如果为 true，无论是否超时都会记录 DEBUG 日志
     * 如果为 false，只在超时时记录 WARN 日志
     * </p>
     *
     * @return 是否记录 DEBUG 日志
     */
    boolean logDebug() default true;

    /**
     * 操作描述
     * <p>
     * 默认值：空字符串（使用方法名作为操作描述）
     * 用于日志输出，便于识别被监控的方法
     * </p>
     *
     * @return 操作描述
     */
    String description() default "";

    /**
     * 是否记录参数
     * <p>
     * 默认值：false
     * 如果为 true，在日志中包含方法参数
     * </p>
     * <p>
     * <b>注意：</b>启用此选项可能会暴露敏感信息，请谨慎使用
     * </p>
     *
     * @return 是否记录参数
     */
    boolean logArgs() default false;

    /**
     * 是否记录返回值
     * <p>
     * 默认值：false
     * 如果为 true，在日志中包含方法返回值
     * </p>
     * <p>
     * <b>注意：</b>启用此选项可能会暴露敏感信息，请谨慎使用
     * </p>
     *
     * @return 是否记录返回值
     */
    boolean logResult() default false;

    /**
     * 是否记录异常堆栈
     * <p>
     * 默认值：true
     * 如果为 false，异常日志只包含异常类型和消息，不包含堆栈
     * </p>
     *
     * @return 是否记录异常堆栈
     */
    boolean logExceptionStackTrace() default true;
}
