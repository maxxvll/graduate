-- ============================================================================
-- V1: 初始化数据库表结构
-- 执行顺序: 1
-- 创建日期: 2026-03-31
-- 描述: 创建所有核心业务表
-- ============================================================================

-- ================================================
-- 1. 用户表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_user (
    id VARCHAR(64) NOT NULL COMMENT '用户唯一ID（主键，建议用雪花ID/UUID）',
    username VARCHAR(64) NOT NULL COMMENT '用户名（登录用，唯一）',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称（聊天展示用）',
    avatar VARCHAR(256) NULL COMMENT '用户头像URL',
    phone VARCHAR(20) NULL COMMENT '手机号（可选）',
    email VARCHAR(128) NULL COMMENT '邮箱（可选）',
    password VARCHAR(128) NOT NULL COMMENT '密码（加密存储）',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '用户状态：1-正常，2-禁用，3-注销',
    ext_info JSON NULL COMMENT '扩展字段（如性别、签名等）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户基础信息表';

CREATE INDEX idx_chat_user_status ON chat_user (status) COMMENT '查询正常/禁用用户';

-- ================================================
-- 2. 聊天会话表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '会话记录主键ID' PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID（单聊=小ID_大ID；群聊=group_群ID）',
    session_type TINYINT NOT NULL COMMENT '会话类型：1-单聊，2-群聊',
    user_id VARCHAR(64) NOT NULL COMMENT '所属用户ID',
    target_id VARCHAR(64) NOT NULL COMMENT '会话对方ID：单聊=对方用户ID；群聊=群ID',
    session_name VARCHAR(128) NOT NULL COMMENT '会话名称：单聊=对方昵称；群聊=群名称',
    session_avatar VARCHAR(256) NULL COMMENT '会话头像：单聊=对方头像；群聊=群头像',
    last_message_id BIGINT UNSIGNED NULL COMMENT '最后一条消息ID',
    last_message_content VARCHAR(256) NULL COMMENT '最后一条消息内容（缩略）',
    last_message_time DATETIME NULL COMMENT '最后一条消息发送时间',
    last_message_sender_id VARCHAR(64) NULL COMMENT '最后一条消息发送人ID',
    unread_count INT UNSIGNED DEFAULT 0 NOT NULL COMMENT '未读消息数',
    is_top TINYINT DEFAULT 0 NOT NULL COMMENT '是否置顶：0-否，1-是',
    is_mute TINYINT DEFAULT 0 NOT NULL COMMENT '是否免打扰：0-否，1-是',
    is_hide TINYINT DEFAULT 0 NOT NULL COMMENT '是否隐藏会话：0-否，1-是',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '软删除标识：0-未删除，1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '会话创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '会话更新时间',
    UNIQUE KEY uk_user_session (user_id, session_id),
    INDEX idx_session_user_top (user_id, is_top, last_message_time DESC) COMMENT '用户会话列表（置顶优先）',
    INDEX idx_session_id (session_id) COMMENT '按会话ID关联消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ================================================
-- 3. 聊天消息表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '消息唯一主键ID' PRIMARY KEY,
    message_no VARCHAR(64) NOT NULL COMMENT '消息唯一业务编号（用于去重）',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    session_type TINYINT NOT NULL COMMENT '会话类型：1-单聊，2-群聊',
    sender_id VARCHAR(64) NOT NULL COMMENT '发信人ID',
    receiver_id VARCHAR(64) NULL COMMENT '收信人ID（单聊必填，群聊为空）',
    message_type TINYINT NOT NULL COMMENT '消息类型：1-文本，2-图片，3-视频，4-音频，5-文件，6-表情，7-系统通知',
    content TEXT NULL COMMENT '消息内容',
    content_replaced TEXT NULL COMMENT '撤回/敏感消息替换内容',
    at_user_ids JSON NULL COMMENT '@的用户ID列表',
    is_at_all TINYINT DEFAULT 0 NOT NULL COMMENT '是否@所有人：0-否，1-是',
    quote_message_id BIGINT UNSIGNED NULL COMMENT '引用/回复的原消息ID',
    file_url TEXT NULL COMMENT '多媒体文件URL',
    file_name VARCHAR(255) NULL COMMENT '文件原始名称',
    file_size BIGINT UNSIGNED NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(32) NULL COMMENT '文件MIME类型',
    duration INT UNSIGNED NULL COMMENT '语音时长（秒）',
    thumbnail_url VARCHAR(256) NULL COMMENT '图片/视频缩略图URL',
    file_expired TINYINT DEFAULT 0 NOT NULL COMMENT '文件是否过期：0-有效，1-过期',
    send_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '消息发送时间',
    revoke_time DATETIME NULL COMMENT '撤回时间',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '消息状态：1-发送成功，2-已读，3-已撤回，4-已删除，5-发送失败',
    operator_id VARCHAR(64) NULL COMMENT '操作人ID（如管理员撤回）',
    ext_info JSON NULL COMMENT '扩展字段',
    is_sensitive TINYINT DEFAULT 0 NOT NULL COMMENT '是否敏感消息：0-否，1-是',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '软删除：0-未删除，1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    UNIQUE KEY uk_message_no (message_no),
    INDEX idx_sender_receiver (sender_id, receiver_id) COMMENT '按收发方筛选',
    INDEX idx_session_time (session_id, send_time) COMMENT '会话消息历史（最核心）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息记录表';

-- ================================================
-- 4. 群组表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_group (
    id VARCHAR(64) NOT NULL COMMENT '群ID（主键，雪花ID/UUID）' PRIMARY KEY,
    group_name VARCHAR(128) NOT NULL COMMENT '群名称',
    group_avatar VARCHAR(256) NULL COMMENT '群头像URL',
    creator_id VARCHAR(64) NOT NULL COMMENT '创建人ID',
    max_member INT UNSIGNED DEFAULT 200 NOT NULL COMMENT '群最大成员数',
    join_type TINYINT DEFAULT 1 NOT NULL COMMENT '加群方式：1-需审核，2-免审核，3-仅邀请',
    notice TEXT NULL COMMENT '群公告',
    is_mute_all TINYINT DEFAULT 0 NOT NULL COMMENT '是否全员禁言：0-否，1-是',
    status TINYINT DEFAULT 1 NOT NULL COMMENT '群状态：1-正常，2-解散，3-封禁',
    ext_info JSON NULL COMMENT '扩展字段',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_id (creator_id) COMMENT '查询用户创建的所有群',
    INDEX idx_chat_group_status (status) COMMENT '筛选正常/解散的群'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊基础信息表';

-- ================================================
-- 5. 群成员表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_group_member (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    group_id VARCHAR(64) NOT NULL COMMENT '群ID',
    user_id VARCHAR(64) NOT NULL COMMENT '成员ID',
    role TINYINT DEFAULT 3 NOT NULL COMMENT '成员角色：1-群主，2-管理员，3-普通成员',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '加入时间',
    inviter_id VARCHAR(64) NULL COMMENT '邀请人ID',
    is_mute TINYINT DEFAULT 0 NOT NULL COMMENT '是否被禁言：0-否，1-是',
    is_quit TINYINT DEFAULT 0 NOT NULL COMMENT '是否退出：0-未退出，1-已退出',
    quit_time DATETIME NULL COMMENT '退出时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    quit_reason VARCHAR(255) NULL COMMENT '移除原因',
    UNIQUE KEY uk_group_user (group_id, user_id) COMMENT '用户在群中仅一条记录',
    INDEX idx_group_role (group_id, role) COMMENT '查询群内群主/管理员',
    INDEX idx_user_id (user_id) COMMENT '查询用户加入的所有群'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员关联表';

-- ================================================
-- 6. 好友申请表
-- ================================================
CREATE TABLE IF NOT EXISTS friend_application (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    target_user_id BIGINT NOT NULL COMMENT '被申请人ID',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '申请状态：0-待处理，1-已通过，2-已拒绝',
    reject_reason VARCHAR(255) NULL COMMENT '拒绝原因',
    remark VARCHAR(200) NULL COMMENT '申请备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '申请创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '申请更新时间',
    UNIQUE KEY uk_applicant_target (applicant_id, target_user_id),
    INDEX idx_applicant_time (applicant_id, create_time DESC),
    INDEX idx_target_time (target_user_id, create_time DESC),
    INDEX idx_status_applicant (status, applicant_id),
    INDEX idx_status_target (status, target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- ================================================
-- 7. 群申请表
-- ================================================
CREATE TABLE IF NOT EXISTS group_application (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    group_id BIGINT NOT NULL COMMENT '目标群聊ID',
    status TINYINT DEFAULT 0 NOT NULL COMMENT '申请状态：0-待处理，1-已通过，2-已拒绝',
    reject_reason VARCHAR(255) NULL COMMENT '拒绝原因',
    operator_id BIGINT NULL COMMENT '操作人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '申请创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '申请更新时间',
    UNIQUE KEY uk_applicant_group (applicant_id, group_id),
    INDEX idx_group_time (group_id, create_time DESC),
    INDEX idx_applicant_time (applicant_id, create_time DESC),
    INDEX idx_applicant_status_group (applicant_id, status, group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群申请表';

-- ================================================
-- 8. 好友关系设置表
-- ================================================
CREATE TABLE IF NOT EXISTS friend_relation_setting (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    owner_user_id BIGINT NOT NULL COMMENT '拥有者用户ID',
    friend_user_id BIGINT NOT NULL COMMENT '好友用户ID',
    remark_name VARCHAR(64) NULL COMMENT '备注名称',
    tag_name VARCHAR(64) NULL COMMENT '标签名称',
    permission_scope TINYINT DEFAULT 0 NOT NULL COMMENT '权限范围',
    is_starred TINYINT DEFAULT 0 NOT NULL COMMENT '是否标星：0-否，1-是',
    is_blacklisted TINYINT DEFAULT 0 NOT NULL COMMENT '是否拉黑：0-否，1-是',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-正常，1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_owner_friend (owner_user_id, friend_user_id),
    INDEX idx_owner_active (owner_user_id, is_deleted, is_blacklisted, is_starred, updated_at DESC),
    INDEX idx_friend_owner (friend_user_id, owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系设置表';

-- ================================================
-- 9. 用户离线消息游标表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_offline_cursor (
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID' PRIMARY KEY,
    last_message_id BIGINT UNSIGNED NULL COMMENT '最后确认拉取的消息ID',
    last_message_time DATETIME NULL COMMENT '最后确认拉取的消息发送时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户离线消息拉取游标表';

-- ================================================
-- 10. 云存储使用统计表
-- ================================================
CREATE TABLE IF NOT EXISTS cloud_storage_usage (
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID' PRIMARY KEY,
    used_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '已使用字节数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户云盘空间统计表';

-- ================================================
-- 11. 云盘文件表
-- ================================================
CREATE TABLE IF NOT EXISTS cloud_file (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL COMMENT '上传用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_path VARCHAR(500) NOT NULL COMMENT '存储路径（MinIO对象名）',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(32) NOT NULL COMMENT '文件MIME类型',
    thumbnail_path VARCHAR(500) NULL COMMENT '缩略图路径',
    parent_id VARCHAR(64) NULL COMMENT '父文件夹ID（根目录为0）',
    is_folder TINYINT DEFAULT 0 NOT NULL COMMENT '是否文件夹：0-文件，1-文件夹',
    is_deleted TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-正常，1-已删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_create_time (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='云盘文件表';

-- ================================================
-- 12. 云盘分享表
-- ================================================
CREATE TABLE IF NOT EXISTS cloud_share (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    file_id VARCHAR(255) NOT NULL COMMENT '文件ID（关联cloud_file.id）',
    user_id BIGINT NOT NULL COMMENT '分享用户ID',
    share_code VARCHAR(6) NOT NULL COMMENT '提取码',
    password VARCHAR(64) NULL COMMENT '访问密码（加密）',
    expire_time DATETIME NULL COMMENT '过期时间',
    download_count INT UNSIGNED DEFAULT 0 COMMENT '下载次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_share_code (share_code),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='云盘文件分享表';

-- ================================================
-- 13. 群文件表
-- ================================================
CREATE TABLE IF NOT EXISTS group_file (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    file_id BIGINT NOT NULL COMMENT '文件ID（关联cloud_file.id）',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小',
    file_type VARCHAR(32) NOT NULL COMMENT '文件MIME类型',
    uploader_id BIGINT NOT NULL COMMENT '上传用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群文件表';

-- ================================================
-- 14. 消息收藏表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    content TEXT COMMENT '收藏内容',
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE/FILE/VOICE',
    file_url VARCHAR(500) COMMENT '文件URL',
    sender_id VARCHAR(64) COMMENT '发送者ID',
    sender_name VARCHAR(64) COMMENT '发送者昵称',
    sender_avatar VARCHAR(256) COMMENT '发送者头像',
    session_id VARCHAR(64) COMMENT '会话ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息收藏表';

-- ================================================
-- 15. 表情包表
-- ================================================
CREATE TABLE IF NOT EXISTS chat_sticker (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '表情名称',
    url VARCHAR(500) NOT NULL COMMENT '表情图片URL',
    category VARCHAR(50) DEFAULT 'custom' COMMENT '表情分类（emoji/custom）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表情包表';

-- ================================================
-- 执行完成
-- ================================================
