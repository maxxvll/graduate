SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'friend_application'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE friend_application CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''申请创建时间''',
    'ALTER TABLE friend_application MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''申请创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'friend_application'
          AND COLUMN_NAME = 'update_time'
    ),
    'ALTER TABLE friend_application CHANGE COLUMN update_time updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''申请更新时间''',
    'ALTER TABLE friend_application MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''申请更新时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'friend_application'
          AND INDEX_NAME = 'idx_target_time'
    ),
    'DROP INDEX idx_target_time ON friend_application',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'friend_application'
          AND INDEX_NAME = 'idx_applicant_time'
    ),
    'DROP INDEX idx_applicant_time ON friend_application',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_target_time ON friend_application(target_user_id, created_at DESC);
CREATE INDEX idx_applicant_time ON friend_application(applicant_id, created_at DESC);

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_application'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE group_application CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''申请创建时间''',
    'ALTER TABLE group_application MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''申请创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_application'
          AND COLUMN_NAME = 'update_time'
    ),
    'ALTER TABLE group_application CHANGE COLUMN update_time updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''申请更新时间''',
    'ALTER TABLE group_application MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''申请更新时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_application'
          AND INDEX_NAME = 'idx_group_time'
    ),
    'DROP INDEX idx_group_time ON group_application',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_application'
          AND INDEX_NAME = 'idx_group_time_status'
    ),
    'DROP INDEX idx_group_time_status ON group_application',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_application'
          AND INDEX_NAME = 'idx_applicant_time'
    ),
    'DROP INDEX idx_applicant_time ON group_application',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_group_time ON group_application(group_id, created_at DESC);
CREATE INDEX idx_group_time_status ON group_application(group_id, status, created_at DESC);
CREATE INDEX idx_applicant_time ON group_application(applicant_id, created_at DESC);
