package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.UnreadSyncQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 未读同步队列Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface UnreadSyncQueueMapper extends BaseMapper<UnreadSyncQueue> {

    /**
     * 批量插入同步任务
     */
    void batchInsert(@Param("list") List<UnreadSyncQueue> queues);

    /**
     * 获取待处理的同步任务
     */
    List<UnreadSyncQueue> selectPendingTasks(@Param("limit") int limit);

    /**
     * 标记任务为已处理
     */
    @Update("UPDATE unread_sync_queue SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    /**
     * 增加重试次数
     */
    @Update("UPDATE unread_sync_queue SET retry_count = retry_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementRetryCount(@Param("id") Long id);
}
