-- Create a moderator user for testing if it doesn't exist
INSERT INTO users (name, password, created_date, role)
SELECT 'moderator', '$2a$12$eGHQWHZTJ/Qa9Vqb3JBxKuQxZ1/KBwPbvZ7XP8aBUiRJp9f4SqJAG', CURRENT_TIMESTAMP, 'MODERATOR'
WHERE NOT EXISTS (SELECT 1
                  FROM users
                  WHERE name = 'moderator');

-- Update the role to MODERATOR if the user already exists
UPDATE users
SET role = 'MODERATOR'
WHERE name = 'moderator'
  AND role != 'MODERATOR';
