-- ============================================================================
-- V13: 消息已读同步机制
-- 执行顺序: 13
-- 创建日期: 2026-03-31
-- 描述: 创建消息已读同步相关表
-- ============================================================================

-- ================================================
-- 1. 消息阅读状态表（记录每条消息的阅读状态）
-- ================================================
CREATE TABLE IF NOT EXISTS message_read_status (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    read_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '阅读时间',
    read_device VARCHAR(64) NULL COMMENT '阅读设备标识',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_message_user (message_id, user_id),
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_session_read_time (session_id, read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息阅读状态表';

-- ================================================
-- 2. 会话阅读进度表（记录每个会话的阅读进度）
-- ================================================
CREATE TABLE IF NOT EXISTS session_read_progress (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    last_read_message_id BIGINT UNSIGNED NULL COMMENT '最后阅读的消息ID',
    last_read_time DATETIME NULL COMMENT '最后阅读时间',
    unread_count INT UNSIGNED DEFAULT 0 NOT NULL COMMENT '当前未读数',
    device_type VARCHAR(32) NULL COMMENT '设备类型：h5/android/ios/pc',
    device_id VARCHAR(64) NULL COMMENT '设备唯一标识',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_session_device (user_id, session_id, device_id),
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话阅读进度表';

-- ================================================
-- 3. 未读消息同步队列表
-- ================================================
CREATE TABLE IF NOT EXISTS unread_sync_queue (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    target_device_id VARCHAR(64) NULL COMMENT '目标设备ID（NULL表示全部设备）',
    sync_type VARCHAR(16) NOT NULL COMMENT '同步类型：SESSION_READ/ALL_READ',
    last_synced_message_id BIGINT UNSIGNED NULL COMMENT '最后同步的消息ID',
    last_synced_time DATETIME NULL COMMENT '最后同步时间',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '状态：0-待处理，1-已处理，2-处理失败',
    retry_count INT DEFAULT 0 NOT NULL COMMENT '重试次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_status (user_id, status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='未读消息同步队列表';

-- ================================================
-- 执行完成
-- ============================================================================
