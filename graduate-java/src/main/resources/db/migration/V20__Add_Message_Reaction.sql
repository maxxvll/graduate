-- 消息反应表
CREATE TABLE IF NOT EXISTS message_reaction (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '反应ID' PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    emoji VARCHAR(16) NOT NULL COMMENT '表情标识',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_message_user_emoji (message_id, user_id, emoji),
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息反应表';

-- 添加外键约束
ALTER TABLE message_reaction ADD CONSTRAINT fk_mr_message
    FOREIGN KEY (message_id) REFERENCES chat_message(id)
    ON DELETE CASCADE ON UPDATE CASCADE;
