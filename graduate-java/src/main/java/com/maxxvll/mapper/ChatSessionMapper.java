package com.maxxvll.mapper;

import com.maxxvll.domain.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxxvll.domain.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
* @author 20570
* @description 针对表【chat_session(聊天会话表（支撑聊天列表）)】的数据库操作Mapper
* @createDate 2026-02-19 12:02:21
* @Entity com.maxxvll.domain.ChatSession
*/
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("""
            <script>
            SELECT *
            FROM chat_session
            WHERE user_id = #{userId}
              AND is_deleted = 0
            <if test='cursorIsTop != null and cursorSortTime != null and cursorId != null'>
              AND (
                    is_top &lt; #{cursorIsTop}
                    OR (is_top = #{cursorIsTop} AND COALESCE(last_message_time, created_at) &lt; #{cursorSortTime})
                    OR (is_top = #{cursorIsTop} AND COALESCE(last_message_time, created_at) = #{cursorSortTime} AND id &lt; #{cursorId})
                  )
            </if>
            ORDER BY is_top DESC, COALESCE(last_message_time, created_at) DESC, id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ChatSession> selectRecentSessions(@Param("userId") String userId,
                                           @Param("limit") int limit,
                                           @Param("cursorIsTop") Integer cursorIsTop,
                                           @Param("cursorSortTime") Date cursorSortTime,
                                           @Param("cursorId") Long cursorId);

    @Select("""
            <script>
            SELECT *
            FROM chat_session
            WHERE user_id = #{userId}
              AND is_deleted = 0
            <if test='cursorUpdatedAt != null and cursorId != null'>
              AND (
                    updated_at &gt; #{cursorUpdatedAt}
                    OR (updated_at = #{cursorUpdatedAt} AND id &gt; #{cursorId})
                  )
            </if>
            ORDER BY updated_at ASC, id ASC
            LIMIT #{limit}
            </script>
            """)
    List<ChatSession> selectUpdatedSessions(@Param("userId") String userId,
                                            @Param("limit") int limit,
                                            @Param("cursorUpdatedAt") Date cursorUpdatedAt,
                                            @Param("cursorId") Long cursorId);

    @Insert("""
            INSERT INTO chat_session (
                session_id,
                session_type,
                user_id,
                target_id,
                session_name,
                session_avatar,
                last_message_id,
                last_message_content,
                last_message_time,
                last_message_sender_id,
                unread_count,
                is_top,
                is_mute,
                is_hide,
                is_deleted,
                created_at,
                updated_at
            ) VALUES (
                #{sessionId},
                #{sessionType},
                #{userId},
                #{targetId},
                #{sessionName},
                #{sessionAvatar},
                #{message.id},
                #{lastMessageContent},
                #{message.sendTime},
                #{message.senderId},
                #{unreadIncrement},
                0,
                0,
                0,
                0,
                #{now},
                #{now}
            )
            ON DUPLICATE KEY UPDATE
                session_type = VALUES(session_type),
                target_id = VALUES(target_id),
                last_message_id = VALUES(last_message_id),
                last_message_content = VALUES(last_message_content),
                last_message_time = VALUES(last_message_time),
                last_message_sender_id = VALUES(last_message_sender_id),
                unread_count = unread_count + #{unreadIncrement},
                is_deleted = 0,
                updated_at = #{now}
            """)
    int upsertSessionAfterSend(@Param("userId") String userId,
                               @Param("targetId") String targetId,
                               @Param("sessionId") String sessionId,
                               @Param("sessionType") Integer sessionType,
                               @Param("sessionName") String sessionName,
                               @Param("sessionAvatar") String sessionAvatar,
                               @Param("lastMessageContent") String lastMessageContent,
                               @Param("unreadIncrement") int unreadIncrement,
                               @Param("message") ChatMessage message,
                               @Param("now") Date now);

    @Update("""
            UPDATE chat_session
            SET last_message_id = #{message.id},
                last_message_content = #{lastMessageContent},
                last_message_time = #{message.sendTime},
                last_message_sender_id = #{message.senderId},
                unread_count = unread_count + CASE WHEN user_id = #{senderId} THEN 0 ELSE 1 END,
                is_deleted = 0,
                updated_at = #{now}
            WHERE session_id = #{message.sessionId}
            """)
    int refreshGroupSessionsAfterSend(@Param("message") ChatMessage message,
                                      @Param("senderId") String senderId,
                                      @Param("lastMessageContent") String lastMessageContent,
                                      @Param("now") Date now);

    @Update("""
            UPDATE chat_session
            SET last_message_content = #{revokedPreview},
                updated_at = #{now}
            WHERE session_id = #{sessionId}
              AND last_message_id = #{messageId}
              AND is_deleted = 0
            """)
    int refreshRevokedMessagePreview(@Param("sessionId") String sessionId,
                                     @Param("messageId") Long messageId,
                                     @Param("revokedPreview") String revokedPreview,
                                     @Param("now") Date now);

    @Update("""
            <script>
            UPDATE chat_session s
            SET last_message_id = (
                    SELECT m.id
                    FROM chat_message m
                    WHERE m.session_id = s.session_id
                      AND m.is_deleted = 0
                      AND m.send_time >= #{cutoff}
                    ORDER BY m.send_time DESC, m.id DESC
                    LIMIT 1
                ),
                last_message_content = (
                    SELECT COALESCE(NULLIF(m.content_replaced, ''), m.content)
                    FROM chat_message m
                    WHERE m.session_id = s.session_id
                      AND m.is_deleted = 0
                      AND m.send_time >= #{cutoff}
                    ORDER BY m.send_time DESC, m.id DESC
                    LIMIT 1
                ),
                last_message_time = (
                    SELECT m.send_time
                    FROM chat_message m
                    WHERE m.session_id = s.session_id
                      AND m.is_deleted = 0
                      AND m.send_time >= #{cutoff}
                    ORDER BY m.send_time DESC, m.id DESC
                    LIMIT 1
                ),
                last_message_sender_id = (
                    SELECT m.sender_id
                    FROM chat_message m
                    WHERE m.session_id = s.session_id
                      AND m.is_deleted = 0
                      AND m.send_time >= #{cutoff}
                    ORDER BY m.send_time DESC, m.id DESC
                    LIMIT 1
                ),
                updated_at = #{now}
            WHERE s.session_id IN
            <foreach collection='sessionIds' item='sessionId' open='(' separator=',' close=')'>
                #{sessionId}
            </foreach>
            </script>
            """)
    int refreshSessionsAfterRetentionCleanup(@Param("sessionIds") List<String> sessionIds,
                                             @Param("cutoff") Date cutoff,
                                             @Param("now") Date now);

    @Update("""
            <script>
            UPDATE chat_session
            SET unread_count = (
                    SELECT COUNT(1)
                    FROM chat_message m
                    WHERE m.session_id = chat_session.session_id
                      AND m.is_deleted = 0
                      AND m.status = #{sendSuccessStatus}
                      AND m.receiver_id = chat_session.user_id
                      AND m.send_time >= #{cutoff}
                ),
                updated_at = #{now}
            WHERE session_type = 1
              AND session_id IN
            <foreach collection='sessionIds' item='sessionId' open='(' separator=',' close=')'>
                #{sessionId}
            </foreach>
            </script>
            """)
    int recalculateSingleUnreadAfterRetentionCleanup(@Param("sessionIds") List<String> sessionIds,
                                                     @Param("sendSuccessStatus") Integer sendSuccessStatus,
                                                     @Param("cutoff") Date cutoff,
                                                     @Param("now") Date now);

    @Update("""
            <script>
            UPDATE chat_session
            SET unread_count = 0,
                updated_at = #{now}
            WHERE last_message_id IS NULL
              AND session_id IN
            <foreach collection='sessionIds' item='sessionId' open='(' separator=',' close=')'>
                #{sessionId}
            </foreach>
            </script>
            """)
    int clearUnreadForEmptySessions(@Param("sessionIds") List<String> sessionIds,
                                    @Param("now") Date now);

    /**
     * 根据会话ID和用户ID查询会话
     */
    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId} AND user_id = #{userId} LIMIT 1")
    ChatSession selectBySessionIdUserId(@Param("sessionId") String sessionId, @Param("userId") String userId);
}




