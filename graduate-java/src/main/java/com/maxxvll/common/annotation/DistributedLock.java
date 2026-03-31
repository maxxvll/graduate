package com.maxxvll.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 基于 Redisson 实现分布式锁，支持自动续期（看门狗机制）
 *
 * 使用示例：
 * <pre>
 * @DistributedLock(key = "user:#{#userId}", waitTime = 5, leaseTime = 10)
 * public void updateUser(String userId, UserUpdateDTO dto) { ... }
 * </pre>
 *
 * @author backend-friend
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式
     * 示例：
     * - "user:lock" 固定 key
     * - "user:#{#userId}" 动态 key，从方法参数获取
     * - "user:#{#dto.userId}" 嵌套属性
     */
    String key();

    /**
     * 锁的 key 前缀，最终 key = prefix + key
     * 默认为 "lock:"
     */
    String prefix() default "lock:";

    /**
     * 获取锁的最大等待时间，默认 10 秒
     * 超过此时间将抛出异常
     */
    long waitTime() default 10;

    /**
     * 锁的持有时间，默认 -1（自动续期，看门狗机制）
     * 设置为正数时，锁将在指定时间后自动释放（不推荐）
     */
    long leaseTime() default -1;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 锁获取失败时的错误消息
     */
    String message() default "操作繁忙，请稍后再试";
}
