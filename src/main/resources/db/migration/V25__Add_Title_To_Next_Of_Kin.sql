-- Migration: Add title column to next_of_kin table
-- Description: Adds a title field (Mr, Mrs, Miss, etc.) to next of kin entries,
--              matching the same functionality as in previous employment and personal information sections.

ALTER TABLE next_of_kin
ADD COLUMN title VARCHAR(50) NULL COMMENT 'Title: Mr, Mrs, Miss, Ms, Dr, Prof, etc.';

