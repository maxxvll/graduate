package com.maxxvll.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRelationSettingSchemaInitializer implements ApplicationRunner {

    private static final String TABLE_NAME = "friend_relation_setting";
    private static final String INDEX_OWNER_ACTIVE = "idx_friend_relation_owner_active";
    private static final String INDEX_FRIEND_OWNER = "idx_friend_relation_friend_owner";
    private static final String INDEX_UNIQUE_OWNER_FRIEND = "uk_owner_friend";

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS friend_relation_setting
            (
                id               bigint unsigned auto_increment COMMENT 'primary key'
                    PRIMARY KEY,
                owner_user_id    bigint                             NOT NULL COMMENT 'owner user id',
                friend_user_id   bigint                             NOT NULL COMMENT 'friend user id',
                remark_name      varchar(64)                        NULL COMMENT 'remark name',
                tag_name         varchar(64)                        NULL COMMENT 'tag name',
                permission_scope tinyint default 0                  NOT NULL COMMENT 'permission scope',
                is_starred       tinyint default 0                  NOT NULL COMMENT 'starred flag',
                is_blacklisted   tinyint default 0                  NOT NULL COMMENT 'blacklist flag',
                is_deleted       tinyint default 0                  NOT NULL COMMENT 'soft delete flag',
                created_at       datetime default CURRENT_TIMESTAMP NOT NULL COMMENT 'created at',
                updated_at       datetime default CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated at',
                CONSTRAINT uk_owner_friend UNIQUE (owner_user_id, friend_user_id)
            ) COMMENT='friend relation setting'
            """;

    private static final String CREATE_OWNER_ACTIVE_INDEX_SQL = """
            CREATE INDEX idx_friend_relation_owner_active
            ON friend_relation_setting (owner_user_id, is_deleted, is_blacklisted, is_starred, updated_at DESC)
            """;

    private static final String CREATE_FRIEND_OWNER_INDEX_SQL = """
            CREATE INDEX idx_friend_relation_friend_owner
            ON friend_relation_setting (friend_user_id, owner_user_id)
            """;

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            boolean tableExists = tableExists(connection, TABLE_NAME);
            statement.execute(CREATE_TABLE_SQL);
            ensureIndexes(connection, statement);
            if (!tableExists) {
                log.info("Created missing table {}", TABLE_NAME);
            }
        } catch (SQLException e) {
            log.error("Failed to initialize schema for {}", TABLE_NAME, e);
        }
    }

    private void ensureIndexes(Connection connection, Statement statement) throws SQLException {
        Set<String> existingIndexes = loadIndexNames(connection, TABLE_NAME);
        if (!existingIndexes.contains(INDEX_OWNER_ACTIVE)) {
            statement.execute(CREATE_OWNER_ACTIVE_INDEX_SQL);
            log.info("Created missing index {}", INDEX_OWNER_ACTIVE);
        }
        if (!existingIndexes.contains(INDEX_FRIEND_OWNER)) {
            statement.execute(CREATE_FRIEND_OWNER_INDEX_SQL);
            log.info("Created missing index {}", INDEX_FRIEND_OWNER);
        }
        if (!existingIndexes.contains(INDEX_UNIQUE_OWNER_FRIEND)) {
            statement.execute("CREATE UNIQUE INDEX " + INDEX_UNIQUE_OWNER_FRIEND
                    + " ON " + TABLE_NAME + " (owner_user_id, friend_user_id)");
            log.info("Created missing unique index {}", INDEX_UNIQUE_OWNER_FRIEND);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet tables = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private Set<String> loadIndexNames(Connection connection, String tableName) throws SQLException {
        Set<String> indexNames = new HashSet<>();
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet indexes = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                if (indexName != null) {
                    indexNames.add(indexName);
                }
            }
        }
        return indexNames;
    }
}
