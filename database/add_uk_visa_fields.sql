-- Add UK VISA specific fields to documents table
-- This migration adds support for UK Home Office VISA documents

-- Add company name field
ALTER TABLE documents ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);

-- Add date of check field
ALTER TABLE documents ADD COLUMN IF NOT EXISTS date_of_check DATE;

-- Add reference number field
ALTER TABLE documents ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100);

-- Add comments for documentation
COMMENT ON COLUMN documents.company_name IS 'Company name from UK VISA details section';
COMMENT ON COLUMN documents.date_of_check IS 'Date of check from UK VISA details section';
COMMENT ON COLUMN documents.reference_number IS 'Reference number from UK VISA details section';

