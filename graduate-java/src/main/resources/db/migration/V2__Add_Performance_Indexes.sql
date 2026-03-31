-- ============================================================================
-- V2: 性能优化索引
-- 执行顺序: 2
-- 创建日期: 2026-03-18
-- 描述: 为核心业务表添加查询优化索引
-- ============================================================================

-- ================================================
-- 1. chat_message 表索引
-- ================================================

-- 复合索引：会话ID + 删除标记 + 发送时间（降序）
-- 用途：按会话查询消息历史，支持分页
CREATE INDEX idx_msg_session_deleted_time
ON chat_message(session_id, is_deleted, send_time DESC)
COMMENT '会话消息分页查询优化';

-- 复合索引：接收者ID + 会话类型 + 状态 + 删除标记 + 发送时间
-- 用途：查询用户未读消息、离线消息
CREATE INDEX idx_msg_receiver_session_status
ON chat_message(receiver_id, session_type, status, is_deleted, send_time)
COMMENT '用户未读/离线消息查询优化';

-- 复合索引：发送者ID + 发送时间（降序）
-- 用途：查询用户发送历史
CREATE INDEX idx_msg_sender_time
ON chat_message(sender_id, send_time DESC)
COMMENT '发送历史查询优化';

-- ================================================
-- 2. chat_session 表索引
-- ================================================

-- 复合索引：用户ID + 删除 + 置顶 + 最后消息时间
-- 用途：会话列表查询（支持置顶优先排序）
CREATE INDEX idx_session_user_deleted_top_time
ON chat_session(user_id, is_deleted, is_top DESC, last_message_time DESC)
COMMENT '用户会话列表查询优化（置顶优先）';

-- 复合索引：用户ID + 未读数
-- 用途：查询有未读消息的会话
CREATE INDEX idx_session_user_unread
ON chat_session(user_id, unread_count)
COMMENT '用户未读消息会话查询';

-- ================================================
-- 3. chat_group_member 表索引
-- ================================================

-- 复合索引：群组ID + 是否退出
-- 用途：查询群组成员（排除已退出成员）
CREATE INDEX idx_group_member_group_deleted
ON chat_group_member(group_id, is_quit)
COMMENT '群组成员查询优化（排除已退出）';

-- 复合索引：用户ID + 是否退出 + 群组ID
-- 用途：批量查询用户加入的活跃群组
CREATE INDEX idx_group_member_user_deleted_group
ON chat_group_member(user_id, is_quit, group_id)
COMMENT '用户群组列表查询优化';

-- ================================================
-- 4. 索引创建完成
-- ================================================

-- 验证索引创建情况
-- SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME
-- FROM INFORMATION_SCHEMA.STATISTICS
-- WHERE TABLE_SCHEMA = 'chat'
-- AND INDEX_NAME LIKE 'idx_%'
-- ORDER BY TABLE_NAME, INDEX_NAME;
