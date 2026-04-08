package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.MessageReaction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MessageReactionMapper extends BaseMapper<MessageReaction> {

    /**
     * 获取消息的所有反应
     */
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId} ORDER BY created_at DESC")
    List<MessageReaction> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 获取用户对某条消息的反应
     */
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId} AND user_id = #{userId}")
    List<MessageReaction> selectByMessageIdAndUserId(@Param("messageId") Long messageId, @Param("userId") Long userId);

    /**
     * 检查用户是否已对消息添加了特定表情
     */
    @Select("SELECT COUNT(*) FROM message_reaction WHERE message_id = #{messageId} AND user_id = #{userId} AND emoji = #{emoji}")
    int countByMessageIdAndUserIdAndEmoji(@Param("messageId") Long messageId, @Param("userId") Long userId, @Param("emoji") String emoji);

    /**
     * 批量获取消息的反应
     */
    @Select("SELECT * FROM message_reaction WHERE message_id IN (${messageIds}) ORDER BY created_at DESC")
    List<MessageReaction> selectByMessageIds(@Param("messageIds") String messageIds);
}
