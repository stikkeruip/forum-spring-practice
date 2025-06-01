-- Create an admin user for testing if it doesn't exist
INSERT INTO users (name, password, created_date, role)
SELECT 'admin', '$2a$12$eGHQWHZTJ/Qa9Vqb3JBxKuQxZ1/KBwPbvZ7XP8aBUiRJp9f4SqJAG', CURRENT_TIMESTAMP, 'ADMIN'
WHERE NOT EXISTS (SELECT 1
                  FROM users
                  WHERE name = 'admin');

-- Update the role to ADMIN if the user already exists
UPDATE users
SET role = 'ADMIN'
WHERE name = 'admin'
  AND role != 'ADMIN';