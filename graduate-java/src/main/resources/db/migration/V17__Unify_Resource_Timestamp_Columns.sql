SET @schema_name = DATABASE();

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chat_favorite'
          AND COLUMN_NAME = 'create_time'
    ),
    'UPDATE chat_favorite SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL',
    'UPDATE chat_favorite SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chat_favorite'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE chat_favorite CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间''',
    'ALTER TABLE chat_favorite MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chat_favorite'
          AND COLUMN_NAME = 'updated_at'
    ),
    'UPDATE chat_favorite SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chat_favorite'
          AND COLUMN_NAME = 'updated_at'
    ),
    'ALTER TABLE chat_favorite MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'ALTER TABLE chat_favorite ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'chat_favorite'
          AND INDEX_NAME = 'idx_create_time'
    ),
    'DROP INDEX idx_create_time ON chat_favorite',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_create_time ON chat_favorite(created_at DESC);

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'cloud_share'
          AND COLUMN_NAME = 'create_time'
    ),
    'UPDATE cloud_share SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL',
    'UPDATE cloud_share SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'cloud_share'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE cloud_share CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间''',
    'ALTER TABLE cloud_share MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'cloud_share'
          AND COLUMN_NAME = 'updated_at'
    ),
    'UPDATE cloud_share SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'cloud_share'
          AND COLUMN_NAME = 'updated_at'
    ),
    'ALTER TABLE cloud_share MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'ALTER TABLE cloud_share ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_file'
          AND COLUMN_NAME = 'create_time'
    ),
    'UPDATE group_file SET create_time = CURRENT_TIMESTAMP WHERE create_time IS NULL',
    'UPDATE group_file SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_file'
          AND COLUMN_NAME = 'create_time'
    ),
    'ALTER TABLE group_file CHANGE COLUMN create_time created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间''',
    'ALTER TABLE group_file MODIFY COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT ''创建时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_file'
          AND COLUMN_NAME = 'updated_at'
    ),
    'UPDATE group_file SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = @schema_name
          AND TABLE_NAME = 'group_file'
          AND COLUMN_NAME = 'updated_at'
    ),
    'ALTER TABLE group_file MODIFY COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''',
    'ALTER TABLE group_file ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
