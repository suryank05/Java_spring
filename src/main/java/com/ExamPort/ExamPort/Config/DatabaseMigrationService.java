package com.ExamPort.ExamPort.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
@Order(1) // Run early in the startup process
public class DatabaseMigrationService implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("=== STARTING DATABASE SCHEMA MIGRATION ===");
        
        try {
            // Create tables if they don't exist (fallback method)
            createTablesIfNotExist();
            
            // Check and fix users table AUTO_INCREMENT
            fixUsersTableAutoIncrement();
            
            // Check and fix verification_tokens table AUTO_INCREMENT
            fixVerificationTokensTableAutoIncrement();
            
            // Fix existing users verification status and roles
            fixExistingUsersVerificationAndRoles();
            
            logger.info("=== DATABASE SCHEMA MIGRATION COMPLETED SUCCESSFULLY ===");
            
        } catch (Exception e) {
            logger.error("Database schema migration failed", e);
            // Don't throw exception to prevent application startup failure
            // The manual ID assignment in AuthController will handle this
        }
    }
    
    private void fixUsersTableAutoIncrement() {
        try {
            logger.info("Checking users table AUTO_INCREMENT configuration...");
            
            // Check if users table exists and get its structure
            boolean tableExists = checkTableExists("users");
            if (!tableExists) {
                logger.info("Users table doesn't exist yet, will be created by Hibernate");
                return;
            }
            
            // Check if id column has AUTO_INCREMENT
            boolean hasAutoIncrement = checkColumnHasAutoIncrement("users", "id");
            
            if (!hasAutoIncrement) {
                logger.info("Users table id column missing AUTO_INCREMENT, fixing...");
                
                // Disable foreign key checks temporarily
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                
                try {
                    // Try to modify the column to add AUTO_INCREMENT
                    jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
                    logger.info("Successfully added AUTO_INCREMENT to users.id column");
                    
                    // Set AUTO_INCREMENT starting value based on existing data
                    Integer maxId = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM users", Integer.class);
                    int nextId = (maxId != null ? maxId + 1 : 1);
                    
                    jdbcTemplate.execute("ALTER TABLE users AUTO_INCREMENT = " + nextId);
                    logger.info("Set users table AUTO_INCREMENT starting value to: {}", nextId);
                    
                } catch (Exception e) {
                    logger.warn("Failed to modify users table directly, will use manual ID assignment: {}", e.getMessage());
                } finally {
                    // Re-enable foreign key checks
                    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
                }
            } else {
                logger.info("Users table id column already has AUTO_INCREMENT");
            }
            
        } catch (Exception e) {
            logger.error("Error checking/fixing users table AUTO_INCREMENT", e);
        }
    }
    
    private void fixVerificationTokensTableAutoIncrement() {
        try {
            logger.info("Checking verification_tokens table AUTO_INCREMENT configuration...");
            
            boolean tableExists = checkTableExists("verification_tokens");
            if (!tableExists) {
                logger.info("Verification_tokens table doesn't exist yet, will be created by Hibernate");
                return;
            }
            
            boolean hasAutoIncrement = checkColumnHasAutoIncrement("verification_tokens", "id");
            
            if (!hasAutoIncrement) {
                logger.info("Verification_tokens table id column missing AUTO_INCREMENT, fixing...");
                
                try {
                    jdbcTemplate.execute("ALTER TABLE verification_tokens MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT");
                    logger.info("Successfully added AUTO_INCREMENT to verification_tokens.id column");
                    
                    // Set AUTO_INCREMENT starting value
                    Integer maxId = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM verification_tokens", Integer.class);
                    int nextId = (maxId != null ? maxId + 1 : 1);
                    
                    jdbcTemplate.execute("ALTER TABLE verification_tokens AUTO_INCREMENT = " + nextId);
                    logger.info("Set verification_tokens table AUTO_INCREMENT starting value to: {}", nextId);
                    
                } catch (Exception e) {
                    logger.warn("Failed to modify verification_tokens table: {}", e.getMessage());
                }
            } else {
                logger.info("Verification_tokens table id column already has AUTO_INCREMENT");
            }
            
        } catch (Exception e) {
            logger.error("Error checking/fixing verification_tokens table AUTO_INCREMENT", e);
        }
    }
    
    private boolean checkTableExists(String tableName) {
        try {
            DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
            ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
            return tables.next();
        } catch (Exception e) {
            logger.error("Error checking if table {} exists", tableName, e);
            return false;
        }
    }
    
    private boolean checkColumnHasAutoIncrement(String tableName, String columnName) {
        try {
            // Query the information_schema to check if column has AUTO_INCREMENT
            String sql = "SELECT EXTRA FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            
            String extra = jdbcTemplate.queryForObject(sql, String.class, tableName, columnName);
            return extra != null && extra.toLowerCase().contains("auto_increment");
            
        } catch (Exception e) {
            logger.error("Error checking AUTO_INCREMENT for {}.{}", tableName, columnName, e);
            return false;
        }
    }
    
    private void fixExistingUsersVerificationAndRoles() {
        try {
            logger.info("Fixing existing users verification status and roles...");
            
            boolean tableExists = checkTableExists("users");
            if (!tableExists) {
                logger.info("Users table doesn't exist yet, skipping user fixes");
                return;
            }
            
            // Fix email_verified column data type if needed
            try {
                jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE");
                logger.info("Fixed email_verified column data type");
            } catch (Exception e) {
                logger.debug("Email_verified column already has correct data type or fix not needed: {}", e.getMessage());
            }
            
            // Count existing users
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (userCount != null && userCount > 0) {
                logger.info("Found {} existing users, updating verification status and roles...", userCount);
                
                // Verify all existing users (they were created before verification was mandatory)
                int verifiedCount = jdbcTemplate.update("UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE");
                logger.info("Verified {} existing users", verifiedCount);
                
                // Fix role formats
                int adminRoleFixed = jdbcTemplate.update("UPDATE users SET role = 'admin' WHERE role IN ('ROLE_ADMIN', 'ADMIN')");
                int studentRoleFixed = jdbcTemplate.update("UPDATE users SET role = 'student' WHERE role IN ('ROLE_STUDENT', 'STUDENT')");
                int instructorRoleFixed = jdbcTemplate.update("UPDATE users SET role = 'instructor' WHERE role IN ('ROLE_INSTRUCTOR', 'INSTRUCTOR')");
                
                if (adminRoleFixed > 0 || studentRoleFixed > 0 || instructorRoleFixed > 0) {
                    logger.info("Fixed roles: {} admin, {} student, {} instructor", adminRoleFixed, studentRoleFixed, instructorRoleFixed);
                }
                
                // Clean up expired tokens
                try {
                    int expiredTokens = jdbcTemplate.update("DELETE FROM verification_tokens WHERE expiry_time < NOW()");
                    if (expiredTokens > 0) {
                        logger.info("Cleaned up {} expired verification tokens", expiredTokens);
                    }
                } catch (Exception e) {
                    logger.debug("Could not clean up expired tokens (table might not exist): {}", e.getMessage());
                }
                
                logger.info("All existing users are now verified and can login!");
            } else {
                logger.info("No existing users found, no fixes needed");
            }
            
        } catch (Exception e) {
            logger.error("Error fixing existing users verification and roles", e);
        }
    }

    /**
     * Create tables if they don't exist (fallback method)
     */
    private void createTablesIfNotExist() {
        try {
            // Create users table if it doesn't exist
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL DEFAULT 'student',
                    full_name VARCHAR(100),
                    phone_number VARCHAR(10),
                    avatar_url VARCHAR(255),
                    gender VARCHAR(10),
                    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                    INDEX idx_username (username),
                    INDEX idx_email (email),
                    INDEX idx_phone (phone_number)
                )
                """;
            
            jdbcTemplate.execute(createUsersTable);
            logger.info("Created users table with proper AUTO_INCREMENT");
            
            // Create verification_tokens table if it doesn't exist
            String createTokensTable = """
                CREATE TABLE IF NOT EXISTS verification_tokens (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    token VARCHAR(255) NOT NULL UNIQUE,
                    user_id BIGINT NOT NULL,
                    expiry_time DATETIME NOT NULL,
                    token_type VARCHAR(50) NOT NULL,
                    used BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_token (token),
                    INDEX idx_user_id (user_id),
                    INDEX idx_token_type (token_type)
                )
                """;
            
            jdbcTemplate.execute(createTokensTable);
            logger.info("Created verification_tokens table with proper AUTO_INCREMENT");
            
            // Create reviews table if it doesn't exist
            String createReviewsTable = """
                CREATE TABLE IF NOT EXISTS reviews (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    content TEXT NOT NULL,
                    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    INDEX idx_user_id (user_id),
                    INDEX idx_approved (is_approved),
                    INDEX idx_active (is_active),
                    INDEX idx_rating (rating),
                    INDEX idx_created_at (created_at)
                )
                """;
            
            jdbcTemplate.execute(createReviewsTable);
            logger.info("Created reviews table with proper AUTO_INCREMENT");
            
        } catch (Exception e) {
            logger.error("Error creating tables", e);
        }
    }
}