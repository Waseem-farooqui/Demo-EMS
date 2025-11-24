-- Add extended profile fields to employees table
ALTER TABLE employees
    ADD COLUMN present_address TEXT NULL,
    ADD COLUMN previous_address TEXT NULL,
    ADD COLUMN has_medical_condition TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN medical_condition_details TEXT NULL,
    ADD COLUMN next_of_kin_name VARCHAR(255) NULL,
    ADD COLUMN next_of_kin_contact VARCHAR(100) NULL,
    ADD COLUMN next_of_kin_address TEXT NULL;

-- Create employment records table for previous employment history
CREATE TABLE IF NOT EXISTS employment_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    job_title VARCHAR(255) NULL,
    employment_period VARCHAR(100) NULL,
    employer_name VARCHAR(255) NULL,
    employer_address TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employment_records_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
        ON DELETE CASCADE
);

