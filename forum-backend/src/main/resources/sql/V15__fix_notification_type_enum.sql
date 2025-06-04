-- Drop the existing enum type and recreate the column as integer
ALTER TABLE notifications DROP COLUMN type;
DROP TYPE notification_type;

-- Add the type column back as integer to work with JPA EnumType.ORDINAL
-- Values correspond to enum ordinal positions:
-- 0 = POST_LIKED
-- 1 = COMMENT_LIKED  
-- 2 = POST_COMMENTED
-- 3 = COMMENT_REPLIED
-- 4 = POST_DELETED_BY_MODERATOR
-- 5 = COMMENT_DELETED_BY_MODERATOR
ALTER TABLE notifications ADD COLUMN type INTEGER NOT NULL DEFAULT 0;