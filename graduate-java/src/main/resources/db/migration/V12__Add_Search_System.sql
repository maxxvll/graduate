-- ============================================================================
-- V12: 搜索功能优化
-- 执行顺序: 12
-- 创建日期: 2026-03-31
-- 描述: 创建搜索相关表
-- ============================================================================

-- ================================================
-- 1. 搜索历史记录表
-- ================================================
CREATE TABLE IF NOT EXISTS search_history (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    search_type VARCHAR(16) NOT NULL COMMENT '搜索类型：MESSAGE/CONTACT/GROUP/ALL',
    keyword VARCHAR(255) NOT NULL COMMENT '搜索关键词',
    search_count INT DEFAULT 1 NOT NULL COMMENT '搜索次数（相同关键词累加）',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_keyword_type (user_id, keyword, search_type),
    INDEX idx_user_created (user_id, created_at DESC) COMMENT '按用户和时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史记录表';

-- ================================================
-- 2. 搜索配置表（可选功能开关）
-- ================================================
CREATE TABLE IF NOT EXISTS search_config (
    id INT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL COMMENT '配置键',
    config_value TEXT NULL COMMENT '配置值',
    description VARCHAR(255) NULL COMMENT '配置描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索配置表';

-- 插入默认配置
INSERT INTO search_config (config_key, config_value, description) VALUES
('search.max_history_count', '50', '搜索历史最大保存条数'),
('search.enable_highlight', 'true', '是否启用搜索结果高亮'),
('search.message.max_results', '100', '消息搜索最大返回条数'),
('search.contact.max_results', '50', '联系人搜索最大返回条数'),
('search.group.max_results', '50', '群组搜索最大返回条数');

-- ================================================
-- 执行完成
-- ============================================================================
