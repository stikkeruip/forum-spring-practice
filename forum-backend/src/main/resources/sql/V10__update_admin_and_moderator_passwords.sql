-- Update the admin user password
UPDATE users
SET password = '$2a$10$7.0BSk.ZEPveb5xBSfPMserVEJZY7pCuvo7mpbvOkBxOBedtJcYi6'
WHERE name = 'admin';

-- Update the moderator user password
UPDATE users
SET password = '$2a$10$ot4QxQNDHmRbAckvi7o1L.02Nf5RcTFMP1BqTRMCHCZxUNmiXevr2'
WHERE name = 'moderator';