-- ============================================================================
-- V4: 申请列表查询优化索引
-- 执行顺序: 4
-- 创建日期: 2026-03-20
-- 描述: 为好友申请和群申请列表查询添加复合索引
-- ============================================================================

-- ================================================
-- friend_application 表索引
-- ================================================

-- 复合索引：接收者ID + 申请时间（降序）
-- 用途：查询用户收到的待处理好友申请
CREATE INDEX idx_friend_app_target_time
ON friend_application(target_user_id, create_time DESC)
COMMENT '好友申请列表查询优化（按接收者）';

-- 复合索引：申请者ID + 申请时间（降序）
-- 用途：查询用户发送的好友申请
CREATE INDEX idx_friend_app_applicant_time
ON friend_application(applicant_id, create_time DESC)
COMMENT '好友申请列表查询优化（按申请者）';

-- 复合索引：状态 + 申请者ID
-- 用途：按状态筛选好友申请
CREATE INDEX idx_friend_app_status_applicant
ON friend_application(status, applicant_id)
COMMENT '好友申请状态筛选优化';

-- 复合索引：状态 + 接收者ID
-- 用途：按状态筛选收到的申请
CREATE INDEX idx_friend_app_status_target
ON friend_application(status, target_user_id)
COMMENT '好友申请状态筛选优化（按接收者）';

-- ================================================
-- group_application 表索引
-- ================================================

-- 复合索引：群组ID + 申请时间（降序）
-- 用途：查询群组收到的入群申请
CREATE INDEX idx_group_app_group_time
ON group_application(group_id, create_time DESC)
COMMENT '群申请列表查询优化（按群组）';

-- 复合索引：群组ID + 状态 + 申请时间
-- 用途：按状态筛选群申请
CREATE INDEX idx_group_app_group_status_time
ON group_application(group_id, status, create_time DESC)
COMMENT '群申请状态筛选优化';

-- 复合索引：申请者ID + 申请时间（降序）
-- 用途：查询用户发送的入群申请
CREATE INDEX idx_group_app_applicant_time
ON group_application(applicant_id, create_time DESC)
COMMENT '入群申请查询优化（按申请者）';

-- 复合索引：申请者ID + 状态 + 群组ID
-- 用途：查询用户入群申请状态
CREATE INDEX idx_group_app_applicant_status_group
ON group_application(applicant_id, status, group_id)
COMMENT '用户入群申请状态查询';
