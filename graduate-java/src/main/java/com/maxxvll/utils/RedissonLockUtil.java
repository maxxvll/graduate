package com.maxxvll.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redisson分布式锁工具类
 * 封装常用的分布式锁操作，包含普通锁、公平锁、读写锁、尝试锁等
 */
@Slf4j
@Component
public class RedissonLockUtil {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取普通可重入锁（阻塞式）
     * @param lockKey 锁名称
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @return 锁实例
     */
    public RLock lock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(leaseTime, timeUnit);
        log.info("获取分布式锁成功，锁key：{}", lockKey);
        return lock;
    }

    /**
     * 获取普通可重入锁（阻塞式，默认30秒自动释放）
     * @param lockKey 锁名称
     * @return 锁实例
     */
    public RLock lock(String lockKey) {
        return lock(lockKey, 30, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取锁（非阻塞式）
     * @param lockKey 锁名称
     * @param waitTime 最大等待时间
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @return true-获取成功，false-获取失败
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean success = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (success) {
                log.info("尝试获取分布式锁成功，锁key：{}", lockKey);
            } else {
                log.warn("尝试获取分布式锁失败，锁key：{}", lockKey);
            }
            return success;
        } catch (InterruptedException e) {
            log.error("尝试获取分布式锁异常，锁key：{}", lockKey, e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return false;
        }
    }

    /**
     * 获取公平锁
     * @param lockKey 锁名称
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @return 锁实例
     */
    public RLock fairLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getFairLock(lockKey);
        lock.lock(leaseTime, timeUnit);
        log.info("获取分布式公平锁成功，锁key：{}", lockKey);
        return lock;
    }

    /**
     * 获取读锁
     * @param lockKey 锁名称
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @return 读锁实例
     */
    public RLock readLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = readWriteLock.readLock();
        readLock.lock(leaseTime, timeUnit);
        log.info("获取分布式读锁成功，锁key：{}", lockKey);
        return readLock;
    }

    /**
     * 获取写锁
     * @param lockKey 锁名称
     * @param leaseTime 锁自动释放时间
     * @param timeUnit 时间单位
     * @return 写锁实例
     */
    public RLock writeLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock(leaseTime, timeUnit);
        log.info("获取分布式写锁成功，锁key：{}", lockKey);
        return writeLock;
    }

    /**
     * 释放锁
     * @param lockKey 锁名称
     */
    public void unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            unlock(lock);
        } catch (Exception e) {
            log.error("释放分布式锁异常，锁key：{}", lockKey, e);
        }
    }

    /**
     * 释放锁（推荐使用，避免锁被其他线程释放）
     * @param lock 锁实例
     */
    public void unlock(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("释放分布式锁成功，锁key：{}", lock.getName());
        }
    }

    /**
     * 自动释放锁的工具方法（try-with-resources 方式）
     * 使用示例：
     * try (RLock lock = redissonLockUtil.autoLock("testKey", 30, TimeUnit.SECONDS)) {
     *     // 业务逻辑
     * }
     */
    public AutoCloseableRLock autoLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        RLock lock = lock(lockKey, leaseTime, timeUnit);
        return new AutoCloseableRLock(lock);
    }

    /**
     * 自动关闭锁的包装类（支持try-with-resources）
     */
    public static class AutoCloseableRLock implements AutoCloseable {
        private final RLock lock;

        public AutoCloseableRLock(RLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("自动释放分布式锁成功，锁key：{}", lock.getName());
            }
        }

        public RLock getLock() {
            return lock;
        }
    }
}