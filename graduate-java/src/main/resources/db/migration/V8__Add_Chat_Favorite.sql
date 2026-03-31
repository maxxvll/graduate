-- ============================================================================
-- V8: 消息收藏功能
-- 执行顺序: 8
-- 创建日期: 2026-03-22
-- 描述: 消息收藏表已在 V1 中创建，此处确认索引配置
-- 备注: chat_favorite 表已在 V1__Init_Schema.sql 中创建
-- ============================================================================

-- 此版本保留用于未来扩展
-- 当前表结构已包含必要的索引：
-- - idx_user_id: 用户收藏列表查询
-- - idx_create_time: 按时间排序

-- 未来可添加：
-- CREATE INDEX idx_favorite_session ON chat_favorite(session_id);
-- CREATE INDEX idx_favorite_message ON chat_favorite(message_id);
