-- V9__Add_User_Role.sql
-- 为用户表添加角色字段，支持基于角色的权限控制

-- 添加用户角色字段
ALTER TABLE chat_user
ADD COLUMN role INT DEFAULT 1 NOT NULL COMMENT '用户角色：1-普通用户，2-管理员，3-超级管理员';

-- 添加角色索引
ALTER TABLE chat_user
ADD INDEX idx_role (role);

-- 为现有管理员用户设置管理员角色（假设 admin 用户存在）
UPDATE chat_user SET role = 2 WHERE username = 'admin';

-- 创建角色表（可选，用于更细粒度的权限管理）
CREATE TABLE IF NOT EXISTS chat_role (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    description VARCHAR(200) COMMENT '角色描述',
    permissions TEXT COMMENT '权限列表（JSON格式）',
    status INT DEFAULT 1 NOT NULL COMMENT '状态：1-正常，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 初始化默认角色
INSERT INTO chat_role (role_name, role_code, description, permissions, status) VALUES
('普通用户', 'USER', '普通用户角色，拥有基本功能权限', '{"user:read": true, "user:write": true, "friend:read": true, "friend:add": true, "friend:delete": true, "group:read": true, "group:create": true, "group:update": true, "message:send": true, "message:read": true, "cloud:read": true, "cloud:upload": true, "cloud:download": true, "cloud:share": true}', 1),
('管理员', 'ADMIN', '管理员角色，拥有大部分管理权限', '{"user:read": true, "user:write": true, "user:ban": true, "friend:*": true, "group:*": true, "message:*": true, "file:*": true, "cloud:*": true, "voice:call": true, "system:log": true}', 1),
('超级管理员', 'SUPER_ADMIN', '超级管理员角色，拥有所有权限', '{"*": true}', 1);

-- 创建用户角色关联表（可选，用于多角色支持）
CREATE TABLE IF NOT EXISTS chat_user_role (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(20) NOT NULL COMMENT '用户ID',
    role_id INT NOT NULL COMMENT '角色ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 将现有管理员迁移到管理员角色
INSERT IGNORE INTO chat_user_role (user_id, role_id)
SELECT id, (SELECT id FROM chat_role WHERE role_code = 'ADMIN') FROM chat_user WHERE role = 2;

-- 创建权限配置表（可选，用于动态权限配置）
CREATE TABLE IF NOT EXISTS chat_permission (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(50) DEFAULT 'menu' COMMENT '权限类型：menu-菜单，button-按钮，api-API',
    parent_id INT DEFAULT 0 COMMENT '父级权限ID',
    path VARCHAR(200) COMMENT '接口路径或菜单路径',
    description VARCHAR(200) COMMENT '权限描述',
    status INT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 初始化权限数据
INSERT INTO chat_permission (permission_code, permission_name, permission_type, path, description) VALUES
-- 用户管理
('user:read', '查看用户', 'button', '/v1/user/*', '查看用户信息'),
('user:write', '编辑用户', 'button', '/v1/user/update', '编辑用户信息'),
('user:delete', '删除用户', 'button', '/v1/user/delete', '删除用户'),
('user:ban', '禁用用户', 'button', '/v1/user/ban', '禁用/启用用户'),

-- 好友管理
('friend:read', '查看好友', 'button', '/v1/friend/*', '查看好友列表'),
('friend:add', '添加好友', 'button', '/v1/friend/apply', '添加好友'),
('friend:delete', '删除好友', 'button', '/v1/friend/delete', '删除好友'),
('friend:blacklist', '拉黑好友', 'button', '/v1/friend/blacklist', '拉黑好友'),

-- 群组管理
('group:read', '查看群组', 'button', '/v1/group/*', '查看群组'),
('group:create', '创建群组', 'button', '/v1/group/create', '创建群组'),
('group:update', '编辑群组', 'button', '/v1/group/update', '编辑群组信息'),
('group:delete', '解散群组', 'button', '/v1/group/delete', '解散群组'),
('group:member:add', '添加成员', 'button', '/v1/group/member/*', '添加群成员'),
('group:member:remove', '移除成员', 'button', '/v1/group/member/remove', '移除群成员'),
('group:member:update', '修改角色', 'button', '/v1/group/member/update', '修改成员角色'),
('group:transfer', '转让群主', 'button', '/v1/group/transfer', '转让群主'),

-- 消息管理
('message:send', '发送消息', 'button', '/v1/chat/send', '发送消息'),
('message:read', '读取消息', 'button', '/v1/chat/*', '读取消息'),
('message:recall', '撤回消息', 'button', '/v1/chat/recall', '撤回消息'),
('message:delete', '删除消息', 'button', '/v1/chat/delete', '删除消息'),

-- 文件管理
('file:upload', '上传文件', 'button', '/v1/file/upload', '上传文件'),
('file:download', '下载文件', 'button', '/v1/file/download', '下载文件'),
('file:delete', '删除文件', 'button', '/v1/file/delete', '删除文件'),

-- 云盘管理
('cloud:read', '查看云盘', 'button', '/v1/cloud/*', '查看云盘'),
('cloud:upload', '上传云盘', 'button', '/v1/cloud/upload', '上传到云盘'),
('cloud:download', '下载云盘', 'button', '/v1/cloud/download', '从云盘下载'),
('cloud:delete', '删除云盘', 'button', '/v1/cloud/delete', '删除云盘文件'),
('cloud:share', '分享云盘', 'button', '/v1/cloud/share', '分享云盘文件'),

-- 系统管理
('system:config', '系统配置', 'menu', '/v1/system/*', '系统配置'),
('system:log', '系统日志', 'menu', '/v1/system/logs', '查看系统日志'),
('system:user:manage', '用户管理', 'menu', '/v1/system/users', '管理系统用户');
