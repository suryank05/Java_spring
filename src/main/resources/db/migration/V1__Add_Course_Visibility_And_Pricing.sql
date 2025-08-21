-- Migration script to add course visibility and pricing features
-- This script extends the existing courses table and creates the enrollments table

-- Check if columns already exist before adding them
SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_name = 'courses' 
               AND column_name = 'visibility' 
               AND table_schema = database());
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column visibility already exists''', 
                   'ALTER TABLE courses ADD COLUMN visibility ENUM(''PRIVATE'', ''PUBLIC'') DEFAULT ''PRIVATE'' AFTER instructor_id');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_name = 'courses' 
               AND column_name = 'pricing' 
               AND table_schema = database());
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column pricing already exists''', 
                   'ALTER TABLE courses ADD COLUMN pricing ENUM(''FREE'', ''PAID'') DEFAULT ''FREE'' AFTER visibility');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_name = 'courses' 
               AND column_name = 'price' 
               AND table_schema = database());
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column price already exists''', 
                   'ALTER TABLE courses ADD COLUMN price DECIMAL(10,2) NULL AFTER pricing');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.columns 
               WHERE table_name = 'courses' 
               AND column_name = 'description' 
               AND table_schema = database());
SET @sqlstmt := IF(@exist > 0, 'SELECT ''Column description already exists''', 
                   'ALTER TABLE courses ADD COLUMN description TEXT NULL AFTER price');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Create enrollments table if it doesn't exist
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ENROLLED', 'PAYMENT_PENDING') DEFAULT 'ENROLLED',
    payment_transaction_id VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate enrollments
    CONSTRAINT unique_enrollment UNIQUE KEY (student_id, course_id),
    
    -- Index for performance
    INDEX idx_student_enrollments (student_id),
    INDEX idx_course_enrollments (course_id),
    INDEX idx_enrollment_status (status)
);

-- Update existing courses to have proper default values (only if columns exist and are null)
UPDATE courses 
SET visibility = 'PRIVATE' 
WHERE visibility IS NULL;

UPDATE courses 
SET pricing = 'FREE' 
WHERE pricing IS NULL;