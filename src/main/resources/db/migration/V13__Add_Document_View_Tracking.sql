-- Add document view tracking columns
ALTER TABLE documents
ADD COLUMN last_viewed_at DATETIME NULL,
ADD COLUMN last_viewed_by VARCHAR(255) NULL;

-- Add index for better query performance
CREATE INDEX idx_documents_last_viewed ON documents(last_viewed_at);
CREATE INDEX idx_documents_last_viewed_by ON documents(last_viewed_by);

