-- ============================================================================
-- V11: 消息推送与通知系统
-- 执行顺序: 11
-- 创建日期: 2026-03-31
-- 描述: 创建通知系统相关表
-- ============================================================================

-- ================================================
-- 1. 用户通知设置表
-- ================================================
CREATE TABLE IF NOT EXISTS user_notification_setting (
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID' PRIMARY KEY,
    -- 免打扰设置
    dnd_enabled TINYINT DEFAULT 0 NOT NULL COMMENT '是否启用免打扰：0-否，1-是',
    dnd_start_time TIME NULL COMMENT '免打扰开始时间',
    dnd_end_time TIME NULL COMMENT '免打扰结束时间',
    -- 通知类型开关
    notify_friend_apply TINYINT DEFAULT 1 NOT NULL COMMENT '好友申请通知：0-关闭，1-开启',
    notify_group_apply TINYINT DEFAULT 1 NOT NULL COMMENT '群申请通知：0-关闭，1-开启',
    notify_group_invite TINYINT DEFAULT 1 NOT NULL COMMENT '群邀请通知：0-关闭，1-开启',
    notify_message TINYINT DEFAULT 1 NOT NULL COMMENT '消息通知：0-关闭，1-开启',
    notify_at TINYINT DEFAULT 1 NOT NULL COMMENT '@提及通知：0-关闭，1-开启',
    notify_system TINYINT DEFAULT 1 NOT NULL COMMENT '系统通知：0-关闭，1-开启',
    -- 推送渠道设置
    push_channel_websocket TINYINT DEFAULT 1 NOT NULL COMMENT 'WebSocket推送：0-关闭，1-开启',
    push_channel_app TINYINT DEFAULT 1 NOT NULL COMMENT 'APP推送：0-关闭，1-开启',
    -- 声音和振动
    sound_enabled TINYINT DEFAULT 1 NOT NULL COMMENT '声音提示：0-关闭，1-开启',
    vibration_enabled TINYINT DEFAULT 1 NOT NULL COMMENT '振动提示：0-关闭，1-开启',
    -- 桌面通知
    desktop_notification TINYINT DEFAULT 1 NOT NULL COMMENT '桌面通知：0-关闭，1-开启',
    -- 创建和更新时间
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知设置表';

-- ================================================
-- 2. 通知记录表
-- ================================================
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '通知ID' PRIMARY KEY,
    notification_type VARCHAR(32) NOT NULL COMMENT '通知类型：FRIEND_APPLY/GROUP_APPLY/GROUP_INVITE/MENTION/SYSTEM/BROADCAST',
    title VARCHAR(128) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    sender_id VARCHAR(64) NULL COMMENT '发送者ID（系统通知为NULL）',
    sender_name VARCHAR(64) NULL COMMENT '发送者名称',
    sender_avatar VARCHAR(256) NULL COMMENT '发送者头像',
    target_type VARCHAR(16) NOT NULL COMMENT '目标类型：USER/GROUP/ALL',
    target_id VARCHAR(64) NULL COMMENT '目标ID（单用户ID、群组ID或NULL表示全员）',
    related_id VARCHAR(64) NULL COMMENT '关联业务ID（如申请ID、消息ID等）',
    related_type VARCHAR(32) NULL COMMENT '关联业务类型',
    priority TINYINT DEFAULT 0 NOT NULL COMMENT '优先级：0-普通，1-重要，2-紧急',
    expire_time DATETIME NULL COMMENT '过期时间（NULL表示不过期）',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '状态：0-未读，1-已读，2-已删除',
    read_time DATETIME NULL COMMENT '阅读时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_target_type_id (target_type, target_id, status, created_at DESC) COMMENT '按目标类型和ID查询通知',
    INDEX idx_notification_type (notification_type, created_at DESC) COMMENT '按通知类型查询',
    INDEX idx_status_created (status, created_at DESC) COMMENT '状态和创建时间复合索引',
    INDEX idx_expire_time (expire_time) COMMENT '过期时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知记录表';

-- ================================================
-- 3. 用户通知关系表（记录用户与通知的阅读关系）
-- ================================================
CREATE TABLE IF NOT EXISTS user_notification_relation (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    notification_id BIGINT UNSIGNED NOT NULL COMMENT '通知ID',
    is_read TINYINT DEFAULT 0 NOT NULL COMMENT '是否已读：0-否，1-是',
    read_time DATETIME NULL COMMENT '阅读时间',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_notification (user_id, notification_id),
    INDEX idx_user_read_status (user_id, is_read, created_at DESC) COMMENT '用户未读通知查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知关系表';

-- ================================================
-- 执行完成
-- ============================================================================
