ALTER TABLE users
ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';

-- Set up an admin user for testing
UPDATE users
SET role = 'ADMIN'
WHERE name = 'admin';