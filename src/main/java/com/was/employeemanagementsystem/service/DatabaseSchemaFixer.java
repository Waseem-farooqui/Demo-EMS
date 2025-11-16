package com.was.employeemanagementsystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixes database schema issues at runtime
 * Removes incorrect columns from rotas table that don't belong there
 */
@Component
@Slf4j
@Order(1) // Run early, before other initializers
public class DatabaseSchemaFixer implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaFixer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("🔧 Checking and fixing database schema...");
            fixRotasTableStructure();
            log.info("✅ Database schema check complete");
        } catch (Exception e) {
            log.error("❌ Error fixing database schema: {}", e.getMessage(), e);
            // Don't fail startup - just log the error
        }
    }

    /**
     * Fix rotas table structure by removing incorrect columns
     */
    private void fixRotasTableStructure() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if rotas table exists
            try (ResultSet tables = metaData.getTables(databaseName, null, "rotas", new String[]{"TABLE"})) {
                if (!tables.next()) {
                    log.debug("rotas table does not exist yet - skipping cleanup");
                    return;
                }
            }

            log.info("🔍 Checking rotas table structure...");

            // Get all columns in rotas table
            List<String> columnsToRemove = new ArrayList<>();
            try (ResultSet columns = metaData.getColumns(databaseName, null, "rotas", null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    // Check if this column should not be in rotas table
                    if (isIncorrectColumn(columnName)) {
                        columnsToRemove.add(columnName);
                    }
                }
            }

            if (columnsToRemove.isEmpty()) {
                log.info("✅ rotas table structure is correct");
                return;
            }

            log.warn("⚠️ Found {} incorrect column(s) in rotas table: {}", columnsToRemove.size(), columnsToRemove);
            log.info("🔧 Removing incorrect columns...");

            // Remove each incorrect column
            for (String columnName : columnsToRemove) {
                try {
                    // First, drop foreign key constraints if they exist
                    dropForeignKeyConstraints(connection, databaseName, "rotas", columnName);
                    
                    // Drop indexes on the column
                    dropIndexes(connection, databaseName, "rotas", columnName);
                    
                    // Drop the column
                    jdbcTemplate.execute("ALTER TABLE rotas DROP COLUMN " + columnName);
                    log.info("  ✓ Removed column: {}", columnName);
                } catch (Exception e) {
                    log.warn("  ⚠️ Could not remove column {}: {}", columnName, e.getMessage());
                }
            }

            log.info("✅ rotas table structure fixed successfully");

        } catch (Exception e) {
            log.error("❌ Error fixing rotas table structure: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fix rotas table structure", e);
        }
    }

    /**
     * Check if a column name is incorrect for rotas table
     */
    private boolean isIncorrectColumn(String columnName) {
        // These columns don't belong to rotas table
        return columnName.equals("employee_id") ||
               columnName.equals("week_start_date") ||
               columnName.equals("week_end_date") ||
               columnName.equals("status") ||
               columnName.equals("created_by") ||
               columnName.equals("created_at") ||
               columnName.equals("updated_at");
    }

    /**
     * Drop foreign key constraints on a column
     */
    private void dropForeignKeyConstraints(Connection connection, String databaseName, String tableName, String columnName) {
        try {
            String sql = "SELECT CONSTRAINT_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE table_schema = ? " +
                        "AND table_name = ? " +
                        "AND column_name = ? " +
                        "AND referenced_table_name IS NOT NULL " +
                        "LIMIT 1";
            
            List<String> fkNames = jdbcTemplate.queryForList(sql, String.class, databaseName, tableName, columnName);
            
            for (String fkName : fkNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + fkName);
                    log.debug("  ✓ Dropped foreign key: {}", fkName);
                } catch (Exception e) {
                    log.debug("  ⚠️ Could not drop foreign key {}: {}", fkName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Error checking foreign keys: {}", e.getMessage());
        }
    }

    /**
     * Drop indexes on a column
     */
    private void dropIndexes(Connection connection, String databaseName, String tableName, String columnName) {
        try {
            String sql = "SELECT DISTINCT index_name " +
                        "FROM INFORMATION_SCHEMA.STATISTICS " +
                        "WHERE table_schema = ? " +
                        "AND table_name = ? " +
                        "AND column_name = ? " +
                        "AND index_name != 'PRIMARY'";
            
            List<String> indexNames = jdbcTemplate.queryForList(sql, String.class, databaseName, tableName, columnName);
            
            for (String indexName : indexNames) {
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
                    log.debug("  ✓ Dropped index: {}", indexName);
                } catch (Exception e) {
                    log.debug("  ⚠️ Could not drop index {}: {}", indexName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Error checking indexes: {}", e.getMessage());
        }
    }
}

