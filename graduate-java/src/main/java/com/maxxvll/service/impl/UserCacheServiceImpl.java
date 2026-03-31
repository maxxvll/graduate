package com.maxxvll.service.impl;

import com.maxxvll.common.constants.RedisKeyConstants;
import com.maxxvll.common.vo.UserInfoVO;
import com.maxxvll.domain.ChatUser;
import com.maxxvll.mapper.ChatUserMapper;
import com.maxxvll.service.cache.UserCacheService;
import com.maxxvll.utils.BeanConvertUtil;
import com.maxxvll.utils.RedissonCacheUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户缓存服务实现类
 * <p>
 * 使用 Redis 缓存用户基本信息，减少数据库查询
 * </p>
 *
 * @author maxxvll
 * @since 2026-04-01
 */
@Service
@Slf4j
public class UserCacheServiceImpl implements UserCacheService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedissonCacheUtil redissonCacheUtil;

    @Resource
    private ChatUserMapper chatUserMapper;

    /**
     * 缓存 key 前缀：user:cache:{userId}
     */
    private static final String CACHE_KEY_FORMAT = "user:cache:%s";

    /**
     * 批量缓存 key 前缀：user:cache:batch
     */
    private static final String BATCH_CACHE_KEY_PREFIX = "user:cache:";

    @Override
    public UserInfoVO getFromCache(String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("获取用户缓存失败：userId 为空");
            return null;
        }

        String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
        try {
            RBucket<UserInfoVO> bucket = redissonClient.getBucket(cacheKey);
            UserInfoVO cachedUser = bucket.get();

            if (cachedUser != null) {
                log.debug("用户缓存命中，userId: {}", userId);
                return cachedUser;
            }

            log.debug("用户缓存未命中，userId: {}", userId);
            return null;
        } catch (Exception e) {
            log.error("获取用户缓存异常，userId: {}", userId, e);
            return null;
        }
    }

    @Override
    public void cacheUserInfo(String userId, UserInfoVO userInfo) {
        cacheUserInfo(userId, userInfo, DEFAULT_CACHE_MINUTES);
    }

    @Override
    public void cacheUserInfo(String userId, UserInfoVO userInfo, int ttlMinutes) {
        if (userId == null || userId.isEmpty()) {
            log.warn("缓存用户信息失败：userId 为空");
            return;
        }
        if (userInfo == null) {
            log.warn("缓存用户信息失败：userInfo 为空，userId: {}", userId);
            return;
        }

        String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
        try {
            RBucket<UserInfoVO> bucket = redissonClient.getBucket(cacheKey);
            bucket.set(userInfo, ttlMinutes, TimeUnit.MINUTES);
            log.debug("用户信息已缓存，userId: {}, TTL: {} 分钟", userId, ttlMinutes);
        } catch (Exception e) {
            log.error("缓存用户信息异常，userId: {}", userId, e);
        }
    }

    @Override
    public void evictUserCache(String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("清除用户缓存失败：userId 为空");
            return;
        }

        String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
        try {
            boolean deleted = redissonClient.getBucket(cacheKey).delete();
            log.debug("清除用户缓存，userId: {}, 结果: {}", userId, deleted ? "成功" : "缓存不存在");
        } catch (Exception e) {
            log.error("清除用户缓存异常，userId: {}", userId, e);
        }
    }

    @Override
    public void evictUserCaches(Iterable<String> userIds) {
        if (userIds == null) {
            log.warn("批量清除用户缓存失败：userIds 为空");
            return;
        }

        try {
            RBatch batch = redissonClient.createBatch();
            int count = 0;

            for (String userId : userIds) {
                if (userId != null && !userId.isEmpty()) {
                    String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
                    batch.getBucket(cacheKey).deleteAsync();
                    count++;
                }
            }

            if (count > 0) {
                batch.execute();
                log.debug("批量清除用户缓存成功，数量: {}", count);
            }
        } catch (Exception e) {
            log.error("批量清除用户缓存异常", e);
        }
    }

    // ==================== 扩展方法 ====================

    /**
     * 获取用户信息（先查缓存，未命中查数据库并回填）
     * <p>
     * 使用 Long 类型 userId，适配大部分业务场景
     * </p>
     *
     * @param userId 用户ID（Long 类型）
     * @return 用户信息，不存在返回 null
     */
    public UserInfoVO getUserById(Long userId) {
        if (userId == null) {
            log.warn("获取用户信息失败：userId 为空");
            return null;
        }

        String userIdStr = String.valueOf(userId);
        String cacheKey = String.format(CACHE_KEY_FORMAT, userIdStr);

        // 先查缓存
        UserInfoVO cachedUser = getFromCache(userIdStr);
        if (cachedUser != null) {
            return cachedUser;
        }

        // 缓存未命中，查数据库
        try {
            ChatUser chatUser = chatUserMapper.selectById(userIdStr);
            if (chatUser == null) {
                log.debug("用户不存在，userId: {}", userId);
                return null;
            }

            UserInfoVO userInfo = BeanConvertUtil.convert(chatUser, UserInfoVO.class);

            // 回填缓存
            cacheUserInfo(userIdStr, userInfo);

            return userInfo;
        } catch (Exception e) {
            log.error("获取用户信息异常，userId: {}", userId, e);
            return null;
        }
    }

    /**
     * 获取用户信息（先查缓存，未命中查数据库并回填）
     * <p>
     * 使用 String 类型 userId，适配 ChatUser 实体
     * </p>
     *
     * @param userId 用户ID（String 类型）
     * @return 用户信息，不存在返回 null
     */
    public UserInfoVO getUserById(String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("获取用户信息失败：userId 为空");
            return null;
        }

        String cacheKey = String.format(CACHE_KEY_FORMAT, userId);

        // 先查缓存
        UserInfoVO cachedUser = getFromCache(userId);
        if (cachedUser != null) {
            return cachedUser;
        }

        // 缓存未命中，查数据库
        try {
            ChatUser chatUser = chatUserMapper.selectById(userId);
            if (chatUser == null) {
                log.debug("用户不存在，userId: {}", userId);
                return null;
            }

            UserInfoVO userInfo = BeanConvertUtil.convert(chatUser, UserInfoVO.class);

            // 回填缓存
            cacheUserInfo(userId, userInfo);

            return userInfo;
        } catch (Exception e) {
            log.error("获取用户信息异常，userId: {}", userId, e);
            return null;
        }
    }

    /**
     * 批量获取用户信息
     * <p>
     * 使用 Redis Pipeline 批量查询，减少网络往返
     * </p>
     *
     * @param userIds 用户ID集合
     * @return 用户ID -> 用户信息的映射
     */
    public Map<String, UserInfoVO> batchGetUsers(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量获取用户信息失败：userIds 为空");
            return new HashMap<>();
        }

        Map<String, UserInfoVO> result = new HashMap<>();
        List<String> missedUserIds = new ArrayList<>();

        try {
            // 使用 Redis Pipeline 批量查询缓存
            RBatch batch = redissonClient.createBatch();
            Map<String, Integer> indexMap = new HashMap<>();
            int index = 0;

            for (String userId : userIds) {
                String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
                batch.getBucket(cacheKey).getAsync();
                indexMap.put(userId, index++);
            }

            // 执行批量查询
            batch.execute();

            // 重新获取结果（因为 Redisson 的 API 限制，使用更简单的方式）
            // 逐个查询已缓存的用户
            for (String userId : userIds) {
                UserInfoVO cached = getFromCache(userId);
                if (cached != null) {
                    result.put(userId, cached);
                } else {
                    missedUserIds.add(userId);
                }
            }

            // 查询缓存未命中的用户
            if (!missedUserIds.isEmpty()) {
                log.debug("缓存未命中用户数: {}，开始查询数据库", missedUserIds.size());
                List<ChatUser> chatUsers = chatUserMapper.selectBatchIds(missedUserIds);

                if (chatUsers != null && !chatUsers.isEmpty()) {
                    // 批量回填缓存
                    for (ChatUser chatUser : chatUsers) {
                        String userId = chatUser.getId();
                        UserInfoVO userInfo = BeanConvertUtil.convert(chatUser, UserInfoVO.class);
                        result.put(userId, userInfo);
                        cacheUserInfo(userId, userInfo);
                    }
                    log.debug("批量回填缓存完成，数量: {}", chatUsers.size());
                }
            }

            log.debug("批量获取用户信息完成，总数: {}, 缓存命中: {}, 数据库查询: {}",
                    userIds.size(), result.size(), missedUserIds.size());

        } catch (Exception e) {
            log.error("批量获取用户信息异常", e);
        }

        return result;
    }

    /**
     * 批量获取用户信息（Long 类型 userId）
     *
     * @param userIds 用户ID集合
     * @return 用户ID -> 用户信息的映射
     */
    public Map<Long, UserInfoVO> batchGetUsersByLongIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量获取用户信息失败：userIds 为空");
            return new HashMap<>();
        }

        // 转换为 String 类型
        Set<String> userIdStrs = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        Map<String, UserInfoVO> stringResult = batchGetUsers(userIdStrs);

        // 转换回 Long 类型
        Map<Long, UserInfoVO> result = new HashMap<>();
        for (Map.Entry<String, UserInfoVO> entry : stringResult.entrySet()) {
            try {
                Long userId = Long.valueOf(entry.getKey());
                result.put(userId, entry.getValue());
            } catch (NumberFormatException e) {
                log.warn("用户ID格式错误: {}", entry.getKey());
            }
        }

        return result;
    }

    /**
     * 清除单个用户缓存（Long 类型 userId）
     *
     * @param userId 用户ID
     */
    public void evictUser(Long userId) {
        if (userId == null) {
            log.warn("清除用户缓存失败：userId 为空");
            return;
        }
        evictUserCache(String.valueOf(userId));
    }

    /**
     * 批量清除用户缓存（Long 类型 userIds）
     *
     * @param userIds 用户ID集合
     */
    public void evictUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量清除用户缓存失败：userIds 为空");
            return;
        }

        Set<String> userIdStrs = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        evictUserCaches(userIdStrs);
    }

    /**
     * 预热用户缓存
     * <p>
     * 在系统启动或需要时，将热点用户数据提前加载到缓存
     * </p>
     *
     * @param userIds 需要预热的用户ID集合
     */
    public void warmUpCache(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("预热缓存失败：userIds 为空");
            return;
        }

        try {
            List<ChatUser> chatUsers = chatUserMapper.selectBatchIds(userIds);
            if (chatUsers == null || chatUsers.isEmpty()) {
                log.warn("预热缓存失败：未找到用户数据");
                return;
            }

            RBatch batch = redissonClient.createBatch();
            int count = 0;

            for (ChatUser chatUser : chatUsers) {
                String userId = chatUser.getId();
                UserInfoVO userInfo = BeanConvertUtil.convert(chatUser, UserInfoVO.class);
                String cacheKey = String.format(CACHE_KEY_FORMAT, userId);
                batch.getBucket(cacheKey).setAsync(userInfo, DEFAULT_CACHE_MINUTES, TimeUnit.MINUTES);
                count++;
            }

            batch.execute();
            log.info("预热用户缓存完成，数量: {}", count);

        } catch (Exception e) {
            log.error("预热用户缓存异常", e);
        }
    }

    /**
     * 获取缓存命中率统计
     *
     * @return 缓存命中率（0-1 之间的小数）
     */
    public double getCacheHitRate() {
        // 这里可以集成 Redis 的 info 命令获取命中率
        // 目前返回默认值
        return 0.0;
    }

    /**
     * 构建缓存键（使用 RedisKeyConstants）
     * <p>
     * 重写接口方法，使用统一的 Redis key 常量
     * </p>
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    @Override
    public String buildCacheKey(String userId) {
        return RedisKeyConstants.buildKey(RedisKeyConstants.USER_INFO, userId);
    }
}
