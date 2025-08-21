-- =====================================================
-- ExamWizards Admin User Creation Script
-- =====================================================
-- This script creates a default admin user for the ExamWizards platform
-- 
-- SECURITY NOTE: Change the default password immediately after first login!
-- 
-- Default Credentials:
-- Username: admin
-- Email: admin@examwizards.com
-- Password: admin123
-- =====================================================

USE examwizards;

-- Check if admin user already exists
SELECT 'Checking for existing admin users...' as status;
SELECT COUNT(*) as existing_admin_count FROM users WHERE role = 'admin';

-- Create admin user (only if none exists)
-- Password hash for 'admin123' using BCrypt with cost factor 10
INSERT INTO users (
    username, 
    email, 
    password, 
    full_name, 
    role, 
    phone_number, 
    gender, 
    is_email_verified, 
    created_at, 
    updated_at
) 
SELECT 
    'admin',
    'admin@examwizards.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjZJUn/3NNVnQxpCI6XQ6zM6FfVlBG', -- BCrypt hash for 'admin123'
    'System Administrator',
    'admin',
    '9999999999',
    'Other',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE role = 'admin' LIMIT 1
);

-- Verify admin user creation
SELECT 'Admin user creation completed!' as status;
SELECT 
    id, 
    username, 
    email, 
    role, 
    full_name, 
    is_email_verified, 
    created_at 
FROM users 
WHERE role = 'admin';

-- Display login instructions
SELECT 
    'ADMIN LOGIN CREDENTIALS' as info,
    'Username: admin' as username_info,
    'Email: admin@examwizards.com' as email_info,
    'Password: admin123' as password_info,
    'WARNING: Change password after first login!' as security_warning;