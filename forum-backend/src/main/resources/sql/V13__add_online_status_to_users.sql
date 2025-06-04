-- Add online status tracking columns to users table
ALTER TABLE users ADD COLUMN is_online BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN last_seen TIMESTAMP;

-- Update existing users to set initial last_seen to created_date
UPDATE users SET last_seen = created_date WHERE last_seen IS NULL;