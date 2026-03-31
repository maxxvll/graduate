package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.CloudStorageUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CloudStorageUsageMapper extends BaseMapper<CloudStorageUsage> {

    /**
     * 查询用户已使用空间
     */
    @Select("SELECT used_bytes FROM cloud_storage_usage WHERE user_id = #{userId}")
    Long selectUsedBytesByUserId(@Param("userId") String userId);

    /**
     * 查询用户存储配额
     */
    @Select("SELECT quota_bytes FROM cloud_storage_usage WHERE user_id = #{userId}")
    Long selectQuotaBytesByUserId(@Param("userId") String userId);

    /**
     * 查询用户存储使用信息
     */
    @Select("SELECT * FROM cloud_storage_usage WHERE user_id = #{userId}")
    CloudStorageUsage selectStorageUsageByUserId(@Param("userId") String userId);

    /**
     * 更新已使用空间
     */
    int upsertUsedBytes(@Param("userId") String userId, @Param("usedBytes") long usedBytes);

    /**
     * 增加/减少使用空间
     */
    int addUsageDelta(@Param("userId") String userId, @Param("delta") long delta);

    /**
     * 更新存储配额
     */
    @Update("""
            UPDATE cloud_storage_usage
            SET quota_bytes = #{quotaBytes}, updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int updateQuotaBytes(@Param("userId") String userId, @Param("quotaBytes") long quotaBytes);

    /**
     * 初始化用户存储配额（如果不存在）
     * 默认配额: 10GB
     */
    @Insert("""
            INSERT INTO cloud_storage_usage (user_id, used_bytes, quota_bytes, created_at, updated_at)
            VALUES (#{userId}, 0, #{quotaBytes}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                used_bytes = COALESCE(used_bytes, 0),
                quota_bytes = COALESCE(quota_bytes, #{quotaBytes}),
                updated_at = NOW()
            """)
    int initStorageQuota(@Param("userId") String userId, @Param("quotaBytes") long quotaBytes);
}
