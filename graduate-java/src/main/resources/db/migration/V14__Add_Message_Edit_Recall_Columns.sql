-- ================================================
-- 为 chat_message 表添加消息编辑和撤回标记字段
-- ================================================

-- 注意：MySQL 8.0.29 以下不支持 ADD COLUMN IF NOT EXISTS 语法
-- 如果列已存在，请跳过该语句或手动删除后重试

-- 消息是否已编辑：0-否，1-是
ALTER TABLE chat_message ADD COLUMN is_edited TINYINT DEFAULT 0 NOT NULL COMMENT '消息是否已编辑：0-否，1-是' AFTER is_sensitive;

-- 最后一次编辑的时间
ALTER TABLE chat_message ADD COLUMN edit_time DATETIME NULL COMMENT '最后一次编辑的时间' AFTER is_edited;

-- 是否已撤回：0-否，1-是
ALTER TABLE chat_message ADD COLUMN is_recalled TINYINT DEFAULT 0 NOT NULL COMMENT '是否已撤回：0-否，1-是' AFTER edit_time;
