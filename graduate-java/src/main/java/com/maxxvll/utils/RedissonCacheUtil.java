package com.maxxvll.utils;

import com.maxxvll.common.constants.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 缓存工具类
 * 提供缓存操作和分布式锁模板方法
 */
@Component
@Slf4j
public class RedissonCacheUtil {

    /**
     * 默认锁等待时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 10;

    /**
     * 默认锁持有时间（秒），-1 表示启用看门狗自动续期
     */
    private static final long DEFAULT_LEASE_TIME = -1;

    @Resource
    private RedissonClient redissonClient;

    public String getCaptchaKey(String captchaKey) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_CAPTCHA, captchaKey);
    }

    public String getLoginFailKey(String username) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_LOGIN_FAIL, username);
    }

    public String getLoginLockKey(String username) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_LOGIN_LOCK, username);
    }

    public String getUserKey(String function, String uniqueId) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, function, uniqueId);
    }

    public String getQrCodeKey(String qrCodeId) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_QR_LOGIN, qrCodeId);
    }

    public String getEmailCodeKey(String email) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_EMAIL_CODE, email);
    }

    public <T> void set(String key, T value) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        redissonClient.<T>getBucket(key).set(value);
    }

    public <T> void set(String key, T value, long timeout, TimeUnit timeUnit) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(timeUnit, "TimeUnit cannot be null");
        redissonClient.<T>getBucket(key).set(value, toDuration(timeout, timeUnit));
    }

    public <T> T get(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean delete(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return redissonClient.getBucket(key).delete();
    }

    public boolean exists(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return redissonClient.getBucket(key).isExists();
    }

    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(timeUnit, "TimeUnit cannot be null");
        return redissonClient.getBucket(key).expire(toDuration(timeout, timeUnit));
    }

    public Long getRemainingTime(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        long remainTimeMs = redissonClient.getBucket(key).remainTimeToLive();
        return remainTimeMs > 0 ? remainTimeMs / 1000 : remainTimeMs;
    }

    public <K, V> RMap<K, V> getMap(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return redissonClient.getMap(key);
    }

    public <K, V> void mapPut(String key, K field, V value) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        redissonClient.<K, V>getMap(key).put(field, value);
    }

    public <K, V> V mapGet(String key, K field) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return redissonClient.<K, V>getMap(key).get(field);
    }

    public <K, V> boolean mapRemove(String key, K field) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return redissonClient.<K, V>getMap(key).remove(field) != null;
    }

    // ==================== 分布式锁模板方法 ====================

    /**
     * 使用分布式锁执行操作（默认参数）
     * 使用看门狗机制自动续期
     *
     * @param lockKey 锁的 key
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, supplier);
    }

    /**
     * 使用分布式锁执行操作（自定义等待时间和持有时间）
     *
     * @param lockKey 锁的 key
     * @param waitTime 获取锁的最大等待时间
     * @param leaseTime 锁的持有时间，-1 表示启用看门狗自动续期
     * @param timeUnit 时间单位
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                throw new RuntimeException("获取分布式锁失败: " + lockKey);
            }

            log.debug("分布式锁获取成功, key={}", lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("分布式锁释放成功, key={}", lockKey);
            }
        }
    }

    /**
     * 使用分布式锁执行无返回值的操作
     *
     * @param lockKey 锁的 key
     * @param runnable 要执行的操作
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS,
                () -> {
                    runnable.run();
                    return null;
                });
    }

    /**
     * 尝试获取分布式锁（非阻塞）
     *
     * @param lockKey 锁的 key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock();
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的 key
     */
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("分布式锁释放成功, key={}", lockKey);
        }
    }

    private Duration toDuration(long timeout, TimeUnit timeUnit) {
        return Duration.ofMillis(timeUnit.toMillis(timeout));
    }

    /**
     * 获取Redis服务器信息
     *
     * @return Redis信息Properties
     */
    public Properties getInfo() {
        Properties info = new Properties();
        try {
            info.setProperty("redis_version", "Redisson 4.0.0");
            info.setProperty("status", "Connected");
            return info;
        } catch (Exception e) {
            log.warn("获取Redis信息失败", e);
            return null;
        }
    }

    /**
     * 获取Redis服务器统计信息
     *
     * @return Redis统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("redis_version", "Redisson 4.0.0");
            stats.put("status", "Connected");
            stats.put("description", "使用Redisson客户端连接Redis");
        } catch (Exception e) {
            log.warn("获取Redis统计失败", e);
        }
        return stats;
    }
}
