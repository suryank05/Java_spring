package com.ExamPort.ExamPort.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the contact_message table exists on application startup
 */
@Component
public class ContactMessageTableInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactMessageTableInitializer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            logger.info("Checking if contact_message table exists...");
            
            // Check if table exists
            String checkTableQuery = "SELECT COUNT(*) FROM information_schema.tables " +
                                   "WHERE table_schema = DATABASE() AND table_name = 'contact_message'";
            
            Integer tableCount = jdbcTemplate.queryForObject(checkTableQuery, Integer.class);
            
            if (tableCount == null || tableCount == 0) {
                logger.info("contact_message table does not exist. Creating it...");
                createContactMessageTable();
                logger.info("contact_message table created successfully!");
            } else {
                logger.info("contact_message table already exists.");
            }
            
        } catch (Exception e) {
            logger.error("Error checking/creating contact_message table: {}", e.getMessage(), e);
            // Don't fail the application startup if table creation fails
        }
    }
    
    private void createContactMessageTable() {
        String createTableSQL = """
            CREATE TABLE contact_message (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL,
                subject VARCHAR(500) NOT NULL,
                message TEXT NOT NULL,
                status ENUM('PENDING', 'IN_PROGRESS', 'REPLIED') NOT NULL DEFAULT 'PENDING',
                submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                admin_response TEXT,
                
                INDEX idx_contact_message_email (email),
                INDEX idx_contact_message_status (status),
                INDEX idx_contact_message_submitted_at (submitted_at)
            )
            """;
        
        jdbcTemplate.execute(createTableSQL);
    }
}