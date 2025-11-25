-- Make job_title optional in employees table
ALTER TABLE employees MODIFY COLUMN job_title VARCHAR(255) NULL;

