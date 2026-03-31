package com.maxxvll.mapper;

import com.maxxvll.domain.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息Mapper
 *
 * @author 20570
 * @since 2026-02-19
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取会话的最后一条消息
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY id DESC LIMIT 1")
    ChatMessage selectLastMessageBySessionId(@Param("sessionId") String sessionId);

    /**
     * 获取指定ID之后的所有消息
     */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND id > #{fromMessageId} AND id <= #{toMessageId} ORDER BY id ASC")
    List<ChatMessage> selectMessagesBetween(@Param("sessionId") String sessionId,
                                            @Param("fromMessageId") Long fromMessageId,
                                            @Param("toMessageId") Long toMessageId);

    /**
     * 获取指定时间之后的未读消息
     */
    @Select("<script>" +
            "SELECT * FROM chat_message WHERE session_id = #{sessionId} AND send_time > #{afterTime} " +
            "AND sender_id != #{userId} ORDER BY id ASC" +
            "</script>")
    List<ChatMessage> selectUnreadMessagesAfterTime(@Param("sessionId") String sessionId,
                                                    @Param("afterTime") long afterTime,
                                                    @Param("userId") String userId);

    /**
     * 获取未同步的消息（介于fromMessageId和toMessageId之间）
     */
    @Select("<script>" +
            "SELECT * FROM chat_message WHERE session_id = #{sessionId} " +
            "<if test='fromMessageId != null'>" +
            " AND id > #{fromMessageId}" +
            "</if>" +
            "<if test='toMessageId != null'>" +
            " AND id &lt;= #{toMessageId}" +
            "</if>" +
            " ORDER BY id ASC" +
            "</script>")
    List<ChatMessage> selectUnsyncedMessages(@Param("sessionId") String sessionId,
                                             @Param("fromMessageId") Long fromMessageId,
                                             @Param("toMessageId") Long toMessageId);
}
