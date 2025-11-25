-- Add blood group and allotted organization fields to employees table
ALTER TABLE employees
    ADD COLUMN blood_group VARCHAR(10) NULL,
    ADD COLUMN allotted_organization VARCHAR(255) NULL;

