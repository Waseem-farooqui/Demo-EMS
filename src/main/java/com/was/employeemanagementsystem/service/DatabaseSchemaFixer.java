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
import java.util.Map;

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
    private final LeaveService leaveService;

    public DatabaseSchemaFixer(DataSource dataSource, JdbcTemplate jdbcTemplate, LeaveService leaveService) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.leaveService = leaveService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            log.info("🔧 Checking and fixing database schema...");
            fixRotasTableStructure();
            fixAttendanceTableStructure();
            fixLeavesTableStructure();
            fixLeaveBalancesTableStructure();
            log.info("✅ Database schema check complete");
        } catch (Exception e) {
            log.error("❌ Error fixing database schema: {}", e.getMessage(), e);
            // Don't fail startup - just log the error
        }
        
        // Initialize leave balances for employees who don't have them
        try {
            log.info("🔍 Checking for employees without leave balances...");
            leaveService.initializeLeaveBalancesForAllEmployees();
        } catch (Exception e) {
            log.error("❌ Error initializing leave balances: {}", e.getMessage(), e);
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

    /**
     * Fix attendance table structure by removing incorrect columns
     */
    private void fixAttendanceTableStructure() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if attendance table exists
            try (ResultSet tables = metaData.getTables(databaseName, null, "attendance", new String[]{"TABLE"})) {
                if (!tables.next()) {
                    log.debug("attendance table does not exist yet - skipping cleanup");
                    return;
                }
            }

            log.info("🔍 Checking attendance table structure...");

            // Get all columns in attendance table
            List<String> columnsToRemove = new ArrayList<>();
            try (ResultSet columns = metaData.getColumns(databaseName, null, "attendance", null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    // Check if this column should not be in attendance table
                    if (isIncorrectAttendanceColumn(columnName)) {
                        columnsToRemove.add(columnName);
                    }
                }
            }

            if (columnsToRemove.isEmpty()) {
                log.info("✅ attendance table structure is correct");
                return;
            }

            log.warn("⚠️ Found {} incorrect column(s) in attendance table: {}", columnsToRemove.size(), columnsToRemove);
            log.info("🔧 Removing incorrect columns...");

            // Remove each incorrect column
            for (String columnName : columnsToRemove) {
                try {
                    // First, drop foreign key constraints if they exist
                    dropForeignKeyConstraints(connection, databaseName, "attendance", columnName);
                    
                    // Drop indexes on the column
                    dropIndexes(connection, databaseName, "attendance", columnName);
                    
                    // Drop the column
                    jdbcTemplate.execute("ALTER TABLE attendance DROP COLUMN " + columnName);
                    log.info("  ✓ Removed column: {}", columnName);
                } catch (Exception e) {
                    log.warn("  ⚠️ Could not remove column {}: {}", columnName, e.getMessage());
                }
            }

            // Ensure work_date and work_location are NOT NULL
            ensureColumnNotNull("attendance", "work_date", "DATE");
            ensureColumnNotNull("attendance", "work_location", "VARCHAR(255)");

            log.info("✅ attendance table structure fixed successfully");

        } catch (Exception e) {
            log.error("❌ Error fixing attendance table structure: {}", e.getMessage(), e);
            // Don't fail startup - just log the error
        }
    }

    /**
     * Check if a column name is incorrect for attendance table
     */
    private boolean isIncorrectAttendanceColumn(String columnName) {
        // These columns don't belong to attendance table
        // Entity uses 'work_date' not 'date', and 'is_active' not 'status'
        return columnName.equals("date") || columnName.equals("status");
    }

    /**
     * Fix leaves table structure by removing incorrect columns
     */
    private void fixLeavesTableStructure() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if leaves table exists
            try (ResultSet tables = metaData.getTables(databaseName, null, "leaves", new String[]{"TABLE"})) {
                if (!tables.next()) {
                    log.debug("leaves table does not exist yet - skipping cleanup");
                    return;
                }
            }

            log.info("🔍 Checking leaves table structure...");

            // Get all columns in leaves table
            List<String> columnsToRemove = new ArrayList<>();
            List<String> allColumns = new ArrayList<>();
            try (ResultSet columns = metaData.getColumns(databaseName, null, "leaves", null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    allColumns.add(columnName + " (" + dataType + ")");
                    
                    // Check if this column should not be in leaves table
                    if (isIncorrectLeavesColumn(columnName, dataType)) {
                        columnsToRemove.add(columnName);
                        log.info("  ⚠️ Found incorrect column: {} ({})", columnName, dataType);
                    }
                }
            }
            
            log.info("  📋 All columns in leaves table: {}", allColumns);

            if (columnsToRemove.isEmpty()) {
                log.info("✅ leaves table structure is correct");
                return;
            }

            log.warn("⚠️ Found {} incorrect column(s) in leaves table: {}", columnsToRemove.size(), columnsToRemove);
            log.info("🔧 Removing incorrect columns...");

            // Remove each incorrect column
            for (String columnName : columnsToRemove) {
                try {
                    // First, drop foreign key constraints if they exist
                    dropForeignKeyConstraints(connection, databaseName, "leaves", columnName);
                    
                    // Drop indexes on the column
                    dropIndexes(connection, databaseName, "leaves", columnName);
                    
                    // Drop the column
                    jdbcTemplate.execute("ALTER TABLE leaves DROP COLUMN " + columnName);
                    log.info("  ✓ Removed column: {}", columnName);
                } catch (Exception e) {
                    log.warn("  ⚠️ Could not remove column {}: {}", columnName, e.getMessage());
                }
            }

            // Ensure correct columns exist with correct types
            ensureColumnExists("leaves", "financial_year", "VARCHAR(20)");
            ensureColumnExists("leaves", "organization_id", "BIGINT");
            ensureColumnExists("leaves", "number_of_days", "INT");
            ensureColumnExists("leaves", "approval_date", "DATE"); // Entity uses approval_date (DATE), not approved_date (DATETIME)
            
            // Fix column types
            fixColumnType("leaves", "financial_year", "VARCHAR(20)"); // Fix from VARCHAR(255) to VARCHAR(20)
            
            // CRITICAL FIX: approved_by must be VARCHAR(255), not BIGINT
            // The entity stores username strings (e.g., "muddisar.mansoor_mshotelman"), not user IDs
            log.info("🔧 Checking approved_by column type...");
            try {
                String checkSql = "SELECT DATA_TYPE, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                                "WHERE table_schema = DATABASE() AND table_name = 'leaves' AND column_name = 'approved_by'";
                List<Map<String, Object>> results = jdbcTemplate.queryForList(checkSql);
                
                if (!results.isEmpty()) {
                    String currentDataType = (String) results.get(0).get("DATA_TYPE");
                    String currentColumnType = (String) results.get(0).get("COLUMN_TYPE");
                    
                    log.info("  📊 Current approved_by type: {} ({})", currentDataType, currentColumnType);
                    
                    // If it's BIGINT or any non-VARCHAR type, fix it
                    if (!"varchar".equalsIgnoreCase(currentDataType)) {
                        log.warn("  ⚠️ approved_by is {} but should be VARCHAR(255) - fixing now...", currentDataType);
                        
                        // Clear any existing numeric values (they're invalid usernames anyway)
                        try {
                            jdbcTemplate.execute("UPDATE leaves SET approved_by = NULL WHERE approved_by IS NOT NULL");
                            log.info("  ✓ Cleared existing approved_by values");
                        } catch (Exception e) {
                            log.warn("  ⚠️ Could not clear approved_by values: {}", e.getMessage());
                        }
                        
                        // Change column type
                        jdbcTemplate.execute("ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL");
                        log.info("  ✅ Fixed approved_by column type to VARCHAR(255)");
                    } else if (!currentColumnType.equalsIgnoreCase("varchar(255)")) {
                        // It's VARCHAR but wrong length
                        jdbcTemplate.execute("ALTER TABLE leaves MODIFY COLUMN approved_by VARCHAR(255) NULL");
                        log.info("  ✅ Fixed approved_by column length to VARCHAR(255)");
                    } else {
                        log.info("  ✅ approved_by column type is already correct (VARCHAR(255))");
                    }
                } else {
                    log.warn("  ⚠️ approved_by column not found - will be created by JPA");
                }
            } catch (Exception e) {
                log.error("  ❌ Error fixing approved_by column: {}", e.getMessage(), e);
            }

            log.info("✅ leaves table structure fixed successfully");

        } catch (Exception e) {
            log.error("❌ Error fixing leaves table structure: {}", e.getMessage(), e);
            // Don't fail startup - just log the error
        }
    }

    /**
     * Check if a column name is incorrect for leaves table
     */
    private boolean isIncorrectLeavesColumn(String columnName, String dataType) {
        // These columns don't belong to leaves table based on Leave entity:
        // - 'year' is incorrect - entity uses 'financial_year'
        // - 'days_taken' is incorrect - entity uses 'number_of_days'
        // - 'approved_date' (DATETIME) is incorrect - entity uses 'approval_date' (DATE)
        // Entity columns: id, employee_id, leave_type, start_date, end_date, number_of_days,
        // reason, status, applied_date, approved_by (String), approval_date (DATE),
        // remarks, medical_certificate, certificate_file_name, certificate_content_type,
        // financial_year, organization_id
        
        // Use case-insensitive comparison for column names
        String colNameLower = columnName.toLowerCase();
        
        // Remove 'year' column
        if (colNameLower.equals("year")) {
            log.debug("  → Marking 'year' for removal (entity uses 'financial_year')");
            return true;
        }
        
        // Remove 'days_taken' column (entity uses 'number_of_days')
        if (colNameLower.equals("days_taken")) {
            log.debug("  → Marking 'days_taken' for removal (entity uses 'number_of_days')");
            return true;
        }
        
        // Only remove 'approved_date' if it's DATETIME (entity uses 'approval_date' DATE)
        if (colNameLower.equals("approved_date") && "DATETIME".equalsIgnoreCase(dataType)) {
            log.debug("  → Marking 'approved_date' (DATETIME) for removal (entity uses 'approval_date' DATE)");
            return true;
        }
        
        return false;
    }

    /**
     * Ensure a column exists, create it if it doesn't
     */
    private void ensureColumnExists(String tableName, String columnName, String columnType) {
        try {
            String sql = "SELECT COUNT(*) " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = ? " +
                        "AND column_name = ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
            
            if (count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                log.info("  ✓ Added column: {} to {} table", columnName, tableName);
            }
        } catch (Exception e) {
            log.debug("Error ensuring column exists: {}", e.getMessage());
        }
    }

    /**
     * Fix leave_balances table structure by removing incorrect columns
     */
    private void fixLeaveBalancesTableStructure() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();

            // Check if leave_balances table exists
            try (ResultSet tables = metaData.getTables(databaseName, null, "leave_balances", new String[]{"TABLE"})) {
                if (!tables.next()) {
                    log.debug("leave_balances table does not exist yet - skipping cleanup");
                    return;
                }
            }

            log.info("🔍 Checking leave_balances table structure...");

            // Get all columns in leave_balances table
            List<String> columnsToRemove = new ArrayList<>();
            try (ResultSet columns = metaData.getColumns(databaseName, null, "leave_balances", null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    // Check if this column should not be in leave_balances table
                    if (isIncorrectLeaveBalanceColumn(columnName)) {
                        columnsToRemove.add(columnName);
                    }
                }
            }

            if (columnsToRemove.isEmpty()) {
                log.info("✅ leave_balances table structure is correct");
            } else {
                log.warn("⚠️ Found {} incorrect column(s) in leave_balances table: {}", columnsToRemove.size(), columnsToRemove);
                log.info("🔧 Removing incorrect columns...");

                // Remove each incorrect column
                for (String columnName : columnsToRemove) {
                    try {
                        // First, drop foreign key constraints if they exist
                        dropForeignKeyConstraints(connection, databaseName, "leave_balances", columnName);
                        
                        // Drop indexes on the column
                        dropIndexes(connection, databaseName, "leave_balances", columnName);
                        
                        // Drop the column
                        jdbcTemplate.execute("ALTER TABLE leave_balances DROP COLUMN " + columnName);
                        log.info("  ✓ Removed column: {}", columnName);
                    } catch (Exception e) {
                        log.warn("  ⚠️ Could not remove column {}: {}", columnName, e.getMessage());
                    }
                }
            }

            // Ensure correct columns exist with correct types
            ensureColumnExists("leave_balances", "financial_year", "VARCHAR(20)");
            ensureColumnExists("leave_balances", "total_allocated", "INT");
            ensureColumnExists("leave_balances", "used_leaves", "INT");
            ensureColumnExists("leave_balances", "remaining_leaves", "INT");
            ensureColumnExists("leave_balances", "organization_id", "BIGINT");

            // Fix column types if they're wrong
            fixColumnType("leave_balances", "financial_year", "VARCHAR(20)");
            fixColumnType("leave_balances", "total_allocated", "INT");
            fixColumnType("leave_balances", "used_leaves", "INT");
            fixColumnType("leave_balances", "remaining_leaves", "INT");

            // Ensure financial_year is NOT NULL
            ensureColumnNotNull("leave_balances", "financial_year", "VARCHAR(20)");
            ensureColumnNotNull("leave_balances", "total_allocated", "INT");
            ensureColumnNotNull("leave_balances", "used_leaves", "INT");
            ensureColumnNotNull("leave_balances", "remaining_leaves", "INT");

            log.info("✅ leave_balances table structure fixed successfully");

        } catch (Exception e) {
            log.error("❌ Error fixing leave_balances table structure: {}", e.getMessage(), e);
            // Don't fail startup - just log the error
        }
    }

    /**
     * Check if a column name is incorrect for leave_balances table
     */
    private boolean isIncorrectLeaveBalanceColumn(String columnName) {
        // These columns don't belong to leave_balances table
        // Entity uses 'financial_year' not 'year'
        // Entity uses 'used_leaves' not 'used'
        // Entity uses 'remaining_leaves' not 'remaining'
        return columnName.equals("year") || 
               columnName.equals("used") || 
               columnName.equals("remaining");
    }

    /**
     * Fix column type if it's incorrect
     */
    private void fixColumnType(String tableName, String columnName, String correctType) {
        try {
            String sql = "SELECT DATA_TYPE, COLUMN_TYPE " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = ? " +
                        "AND column_name = ?";
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, tableName, columnName);
            
            if (!results.isEmpty()) {
                String currentType = (String) results.get(0).get("COLUMN_TYPE");
                // Check if type needs to be changed
                // For VARCHAR, check if length is different
                // For INT vs DECIMAL, always fix
                if (correctType.startsWith("VARCHAR")) {
                    if (!currentType.toUpperCase().startsWith("VARCHAR")) {
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + correctType);
                        log.info("  ✓ Changed {} column type to {} in {} table", columnName, correctType, tableName);
                    } else if (!currentType.equals(correctType)) {
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + correctType);
                        log.info("  ✓ Changed {} column type from {} to {} in {} table", columnName, currentType, correctType, tableName);
                    }
                } else if (correctType.equals("INT")) {
                    if (!currentType.toUpperCase().contains("INT")) {
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + correctType + " NOT NULL DEFAULT 0");
                        log.info("  ✓ Changed {} column type to {} in {} table", columnName, correctType, tableName);
                    }
                } else if (correctType.equals("BIGINT")) {
                    if (!currentType.toUpperCase().contains("BIGINT")) {
                        jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + correctType);
                        log.info("  ✓ Changed {} column type to {} in {} table", columnName, correctType, tableName);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error fixing column type: {}", e.getMessage());
        }
    }

    /**
     * Ensure a column is NOT NULL
     */
    private void ensureColumnNotNull(String tableName, String columnName, String columnType) {
        try {
            String sql = "SELECT IS_NULLABLE " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = ? " +
                        "AND column_name = ?";
            
            String isNullable = jdbcTemplate.queryForObject(sql, String.class, tableName, columnName);
            
            if ("YES".equals(isNullable)) {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + columnType + " NOT NULL");
                log.info("  ✓ Set {} column to NOT NULL in {} table", columnName, tableName);
            }
        } catch (Exception e) {
            log.debug("Error ensuring column NOT NULL: {}", e.getMessage());
        }
    }
}

