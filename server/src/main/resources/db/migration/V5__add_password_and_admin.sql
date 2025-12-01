-- Add password column
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);
