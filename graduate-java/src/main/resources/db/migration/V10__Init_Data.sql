-- ============================================================================
-- V10: 初始化测试数据
-- 执行顺序: 10
-- 创建日期: 2026-03-31
-- 描述: 初始化必要的测试数据（仅用于开发/测试环境）
-- 注意: 生产环境请勿执行此脚本！
-- ============================================================================

-- ================================================
-- 环境检查
-- ================================================
-- 只有在非生产环境才插入测试数据
-- IF @@hostname NOT IN ('prod-server-1', 'prod-server-2') THEN

-- ================================================
-- 1. 创建测试用户
-- ================================================

-- 测试用户1（普通用户）
INSERT INTO chat_user (id, username, nickname, avatar, password, status, created_at, updated_at)
VALUES ('1001', 'testuser1', '测试用户1', 'https://api.dicebear.com/7.x/avataaars/svg?seed=test1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- 测试用户2（普通用户）
INSERT INTO chat_user (id, username, nickname, avatar, password, status, created_at, updated_at)
VALUES ('1002', 'testuser2', '测试用户2', 'https://api.dicebear.com/7.x/avataaars/svg?seed=test2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- 测试用户3（普通用户）
INSERT INTO chat_user (id, username, nickname, avatar, password, status, created_at, updated_at)
VALUES ('1003', 'testuser3', '测试用户3', 'https://api.dicebear.com/7.x/avataaars/svg?seed=test3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- 管理员用户
INSERT INTO chat_user (id, username, nickname, avatar, password, status, created_at, updated_at)
VALUES ('9999', 'admin', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- ================================================
-- 2. 创建测试群组
-- ================================================

-- 测试群组1
INSERT INTO chat_group (id, group_name, group_avatar, creator_id, max_member, join_type, status, created_at, updated_at)
VALUES ('G001', '测试群聊1', 'https://api.dicebear.com/7.x/identicon/svg?seed=group1', '1001', 200, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name);

-- 测试群组2
INSERT INTO chat_group (id, group_name, group_avatar, creator_id, max_member, join_type, status, created_at, updated_at)
VALUES ('G002', '测试群聊2', 'https://api.dicebear.com/7.x/identicon/svg?seed=group2', '1002', 500, 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE group_name = VALUES(group_name);

-- ================================================
-- 3. 添加测试群组成员
-- ================================================

-- 群组1成员
INSERT INTO chat_group_member (group_id, user_id, role, join_time, is_mute, is_quit, created_at, updated_at)
VALUES ('G001', '1001', 1, NOW(), 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO chat_group_member (group_id, user_id, role, join_time, is_mute, is_quit, created_at, updated_at)
VALUES ('G001', '1002', 2, NOW(), 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO chat_group_member (group_id, user_id, role, join_time, is_mute, is_quit, created_at, updated_at)
VALUES ('G001', '1003', 3, NOW(), 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- 群组2成员
INSERT INTO chat_group_member (group_id, user_id, role, join_time, is_mute, is_quit, created_at, updated_at)
VALUES ('G002', '1002', 1, NOW(), 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO chat_group_member (group_id, user_id, role, join_time, is_mute, is_quit, created_at, updated_at)
VALUES ('G002', '1003', 3, NOW(), 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE role = VALUES(role);

-- ================================================
-- 4. 创建测试会话
-- ================================================

-- 用户1与用户2的单聊会话
INSERT INTO chat_session (session_id, session_type, user_id, target_id, session_name, session_avatar, unread_count, is_top, is_mute, is_deleted, created_at, updated_at)
VALUES ('1001_1002', 1, '1001', '1002', '测试用户2', 'https://api.dicebear.com/7.x/avataaars/svg?seed=test2', 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE session_name = VALUES(session_name);

INSERT INTO chat_session (session_id, session_type, user_id, target_id, session_name, session_avatar, unread_count, is_top, is_mute, is_deleted, created_at, updated_at)
VALUES ('1001_1002', 1, '1002', '1001', '测试用户1', 'https://api.dicebear.com/7.x/avataaars/svg?seed=test1', 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE session_name = VALUES(session_name);

-- 用户1的群聊会话
INSERT INTO chat_session (session_id, session_type, user_id, target_id, session_name, session_avatar, unread_count, is_top, is_mute, is_deleted, created_at, updated_at)
VALUES ('group_G001', 2, '1001', 'G001', '测试群聊1', 'https://api.dicebear.com/7.x/identicon/svg?seed=group1', 0, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE session_name = VALUES(session_name);

-- 用户2的群聊会话
INSERT INTO chat_session (session_id, session_type, user_id, target_id, session_name, session_avatar, unread_count, is_top, is_mute, is_deleted, created_at, updated_at)
VALUES ('group_G001', 2, '1002', 'G001', '测试群聊1', 'https://api.dicebear.com/7.x/identicon/svg?seed=group1', 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE session_name = VALUES(session_name);

-- 用户3的群聊会话
INSERT INTO chat_session (session_id, session_type, user_id, target_id, session_name, session_avatar, unread_count, is_top, is_mute, is_deleted, created_at, updated_at)
VALUES ('group_G001', 2, '1003', 'G001', '测试群聊1', 'https://api.dicebear.com/7.x/identicon/svg?seed=group1', 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE session_name = VALUES(session_name);

-- ================================================
-- 5. 创建测试消息
-- ================================================

-- 单聊消息
INSERT INTO chat_message (message_no, session_id, session_type, sender_id, receiver_id, message_type, content, status, send_time, is_deleted, created_at, updated_at)
VALUES
    ('msg_1001_1002_001', '1001_1002', 1, '1001', '1002', 1, '你好，这是测试消息1', 1, NOW(), 0, NOW(), NOW()),
    ('msg_1001_1002_002', '1001_1002', 1, '1002', '1001', 1, '你好，已收到', 1, NOW(), 0, NOW(), NOW()),
    ('msg_1001_1002_003', '1001_1002', 1, '1001', '1002', 1, '测试消息2', 1, NOW(), 0, NOW(), NOW()),
    ('msg_1001_1002_004', '1001_1002', 1, '1002', '1001', 1, '收到', 1, NOW(), 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- 群聊消息
INSERT INTO chat_message (message_no, session_id, session_type, sender_id, receiver_id, message_type, content, status, send_time, is_deleted, created_at, updated_at)
VALUES
    ('msg_group_G001_001', 'group_G001', 2, '1001', NULL, 1, '大家好，这是群聊测试消息', 1, NOW(), 0, NOW(), NOW()),
    ('msg_group_G001_002', 'group_G001', 2, '1002', NULL, 1, '收到，欢迎！', 1, NOW(), 0, NOW(), NOW()),
    ('msg_group_G001_003', 'group_G001', 2, '1003', NULL, 1, '我也来了', 1, NOW(), 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- ================================================
-- 6. 创建好友关系
-- ================================================

-- 用户1与用户2的好友关系
INSERT INTO friend_relation_setting (owner_user_id, friend_user_id, permission_scope, is_starred, is_blacklisted, is_deleted, created_at, updated_at)
VALUES (1001, 1002, 0, 1, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE remark_name = VALUES(remark_name);

INSERT INTO friend_relation_setting (owner_user_id, friend_user_id, permission_scope, is_starred, is_blacklisted, is_deleted, created_at, updated_at)
VALUES (1002, 1001, 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE remark_name = VALUES(remark_name);

-- 用户1与用户3的好友关系
INSERT INTO friend_relation_setting (owner_user_id, friend_user_id, permission_scope, is_starred, is_blacklisted, is_deleted, created_at, updated_at)
VALUES (1001, 1003, 0, 0, 0, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE remark_name = VALUES(remark_name);

-- ================================================
-- 7. 初始化云存储配额
-- ================================================

INSERT INTO cloud_storage_usage (user_id, used_bytes, created_at, updated_at)
VALUES
    ('1001', 0, NOW(), NOW()),
    ('1002', 0, NOW(), NOW()),
    ('1003', 0, NOW(), NOW()),
    ('9999', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE used_bytes = VALUES(used_bytes);

-- ================================================
-- 8. 验证数据
-- ================================================

SELECT '=== 用户数据 ===' AS info;
SELECT id, username, nickname, status FROM chat_user ORDER BY id;

SELECT '=== 群组数据 ===' AS info;
SELECT id, group_name, creator_id, status FROM chat_group ORDER BY id;

SELECT '=== 群组成员 ===' AS info;
SELECT group_id, user_id, role FROM chat_group_member ORDER BY group_id, user_id;

SELECT '=== 会话数据 ===' AS info;
SELECT session_id, session_type, user_id, target_id FROM chat_session ORDER BY session_id;

-- END IF;

-- ================================================
-- 执行完成
-- ================================================
-- 测试数据初始化完成
-- 测试账号密码（BCrypt加密）：password123
