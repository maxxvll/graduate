-- 好友分组表
CREATE TABLE IF NOT EXISTS friend_group (
    id BIGINT UNSIGNED AUTO_INCREMENT COMMENT '分组ID' PRIMARY KEY,
    owner_user_id BIGINT NOT NULL COMMENT '拥有者用户ID',
    group_name VARCHAR(32) NOT NULL COMMENT '分组名称',
    group_order INT DEFAULT 0 COMMENT '排序（数字越小越靠前）',
    is_default TINYINT DEFAULT 0 NOT NULL COMMENT '是否默认分组：0-否，1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_owner_group (owner_user_id, group_name),
    INDEX idx_owner_order (owner_user_id, group_order, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友分组表';

-- 修改好友关系表，添加分组字段
ALTER TABLE friend_relation_setting ADD COLUMN group_id BIGINT UNSIGNED NULL COMMENT '所属分组ID' AFTER tag_name;

-- 添加外键约束
ALTER TABLE friend_relation_setting ADD CONSTRAINT fk_frs_group
    FOREIGN KEY (group_id) REFERENCES friend_group(id)
    ON DELETE SET NULL ON UPDATE CASCADE;
