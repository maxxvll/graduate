SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_operation_log'
          AND COLUMN_NAME = 'create_time'
    ),
    'UPDATE sys_operation_log SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL',
    'UPDATE sys_operation_log SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_operation_log'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE sys_operation_log CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间''',
    'ALTER TABLE sys_operation_log MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND COLUMN_NAME = 'create_time'
    ),
    'UPDATE sys_audit_log SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL',
    'UPDATE sys_audit_log SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'sys_audit_log'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE sys_audit_log CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间''',
    'ALTER TABLE sys_audit_log MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
