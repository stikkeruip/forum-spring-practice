-- Friend notification types are handled via JPA EnumType.ORDINAL
-- The new enum values will be:
-- 6 = POST_RESTORED_BY_MODERATOR
-- 7 = FRIEND_REQUEST_SENT
-- 8 = FRIEND_REQUEST_ACCEPTED
-- 9 = FRIEND_REQUEST_DECLINED

-- This migration is a placeholder as the enum handling is done in Java
-- No SQL changes needed since we use integer ordinal values