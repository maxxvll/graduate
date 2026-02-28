package com.maxxvll.utils;

import com.maxxvll.common.RedisKeyConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedissonCacheUtil {
    @Resource
    private RedissonClient redissonClient;
    public String getCaptchaKey(String captchakay) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_CAPTCHA, captchakay);
    }
    public String getLoginFailKey(String username) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_LOGIN_FAIL, username);
    }
    public String getLoginLockKey(String username) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, RedisKeyConstants.USER_LOGIN_LOCK, username);
    }
    public String getUserKey(String function,String uniqueId){
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX, function, uniqueId);
    }
    public String getQrCodeKey(String qrCOdeId){
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_PREFIX,RedisKeyConstants.USER_QR_LOGIN, qrCOdeId);
    }
    public Long getRemainingTime(String key){
        return redissonClient.getBucket(key).remainTimeToLive()/1000;
    }
    /**
     * 设置缓存（无过期时间）
     */
    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
        log.debug("Redis设置缓存成功，key：{}", key);
    }

    /**
     * 设置缓存（带过期时间）
     */
    public <T> void set(String key, T value, long timeout, TimeUnit timeUnit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, timeUnit);
        log.debug("Redis设置缓存成功，key：{}，过期时间：{} {}", key, timeout, timeUnit);
    }

    /**
     * 获取缓存
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        log.debug("Redis获取缓存，key：{}，value：{}", key, value);
        return value;
    }

    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        boolean deleted = redissonClient.getBucket(key).delete();
        log.debug("Redis删除缓存，key：{}，结果：{}", key, deleted);
        return deleted;
    }

    /**
     * 判断 Key 是否存在
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).expire(timeout, timeUnit);
    }

    // ==================== Map 操作 (Hash 类型) ====================

    /**
     * 获取 Map 对象
     */
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    /**
     * 向 Map 中添加值
     */
    public <K, V> void mapPut(String key, K field, V value) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * 从 Map 中获取值
     */
    public <K, V> V mapGet(String key, K field) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.get(field);
    }











}
