package com.maxxvll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.ChatOfflineCursor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface ChatOfflineCursorMapper extends BaseMapper<ChatOfflineCursor> {

    @Insert("""
            INSERT INTO chat_offline_cursor (
                user_id,
                last_message_id,
                last_message_time,
                created_at,
                updated_at
            ) VALUES (
                #{userId},
                #{lastMessageId},
                #{lastMessageTime},
                #{now},
                #{now}
            )
            ON DUPLICATE KEY UPDATE
                last_message_id = VALUES(last_message_id),
                last_message_time = VALUES(last_message_time),
                updated_at = #{now}
            """)
    int upsertCursor(@Param("userId") String userId,
                     @Param("lastMessageId") Long lastMessageId,
                     @Param("lastMessageTime") Date lastMessageTime,
                     @Param("now") Date now);
}
