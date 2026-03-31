package com.maxxvll.service.cache;

import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatUser;

/**
 * 用户缓存服务接口
 * 统一管理用户信息的缓存策略
 *
 * @author maxxvll
 * @since 2026-03-31
 */
public interface UserCacheService {

    /**
     * 缓存键前缀
     */
    String CACHE_KEY_PREFIX = "user:";

    /**
     * 默认缓存时间（分钟）
     */
    int DEFAULT_CACHE_MINUTES = 30;

    /**
     * 从缓存获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息，如果缓存不存在返回null
     */
    UserInfoVO getFromCache(String userId);

    /**
     * 将用户信息存入缓存
     *
     * @param userId 用户ID
     * @param userInfo 用户信息
     */
    void cacheUserInfo(String userId, UserInfoVO userInfo);

    /**
     * 将用户信息存入缓存（指定TTL）
     *
     * @param userId 用户ID
     * @param userInfo 用户信息
     * @param ttlMinutes 缓存时间（分钟）
     */
    void cacheUserInfo(String userId, UserInfoVO userInfo, int ttlMinutes);

    /**
     * 清除用户信息缓存
     *
     * @param userId 用户ID
     */
    void evictUserCache(String userId);

    /**
     * 批量清除用户缓存
     *
     * @param userIds 用户ID列表
     */
    void evictUserCaches(Iterable<String> userIds);

    /**
     * 构建缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    default String buildCacheKey(String userId) {
        return CACHE_KEY_PREFIX + "info:" + userId;
    }
}
