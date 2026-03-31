package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.MessageReadStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息阅读状态Mapper
 *
 * @author Claude Code
 * @since 2026-03-31
 */
@Mapper
public interface MessageReadStatusMapper extends BaseMapper<MessageReadStatus> {

    /**
     * 批量插入或更新阅读状态
     */
    void batchInsertOrUpdate(@Param("list") List<MessageReadStatus> statuses);

    /**
     * 获取用户对指定消息的阅读状态
     */
    @Select("SELECT * FROM message_read_status WHERE message_id = #{messageId} AND user_id = #{userId} LIMIT 1")
    MessageReadStatus selectByMessageAndUser(@Param("messageId") Long messageId, @Param("userId") String userId);

    /**
     * 获取用户对会话中消息的阅读状态列表
     */
    List<MessageReadStatus> selectByUserAndSession(@Param("userId") String userId, @Param("sessionId") String sessionId);
}
