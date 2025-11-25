-- Add holiday form fields to leaves table
ALTER TABLE leaves
ADD COLUMN holiday_form LONGBLOB,
ADD COLUMN holiday_form_file_name VARCHAR(255),
ADD COLUMN holiday_form_content_type VARCHAR(100);

