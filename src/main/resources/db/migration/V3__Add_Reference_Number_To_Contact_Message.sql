-- Add reference_number column to contact_message table
ALTER TABLE contact_message ADD COLUMN reference_number VARCHAR(50) UNIQUE;

-- Create index for faster lookups
CREATE INDEX idx_contact_message_reference_number ON contact_message(reference_number);

-- Update existing records with generated reference numbers
-- This will generate reference numbers for existing records
UPDATE contact_message 
SET reference_number = CONCAT('REF-', 
    DATE_FORMAT(submitted_at, '%Y%m%d'), '-',
    DATE_FORMAT(submitted_at, '%H%i%s'), '-',
    LPAD(id % 1000, 3, '0'))
WHERE reference_number IS NULL;

-- Make the column NOT NULL after updating existing records
ALTER TABLE contact_message MODIFY COLUMN reference_number VARCHAR(50) NOT NULL UNIQUE;