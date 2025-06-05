-- Migration to fix any friendship status data that may have been stored as integers
-- due to the previous @Enumerated(EnumType.ORDINAL) mapping

-- This migration handles potential data inconsistency where status might have been 
-- stored as integers (0, 1, 2, 3) instead of enum strings due to JPA mapping issue

-- Check if there's any numeric data in the status column and convert it
-- Note: This will only run if there are rows with numeric-like status values

-- First, let's ensure the table structure is correct
-- The enum should already be defined, but we'll make sure the mapping is clear:
-- 0 -> 'PENDING'
-- 1 -> 'ACCEPTED' 
-- 2 -> 'DECLINED'
-- 3 -> 'BLOCKED'

-- Since PostgreSQL enum comparison with integers should fail (which is what we saw),
-- any existing data should already be in the correct enum format.
-- This migration is primarily for documentation and future safety.

-- Add a comment to the table to document the enum mapping
COMMENT ON COLUMN friendships.status IS 'Friendship status: PENDING, ACCEPTED, DECLINED, BLOCKED';

-- Ensure all existing records have valid enum values
-- This will fail if there are any invalid values, alerting us to data issues
DO $$
BEGIN
    -- Validate that all status values are valid enum values
    IF EXISTS (
        SELECT 1 FROM friendships 
        WHERE status NOT IN ('PENDING'::friendship_status, 'ACCEPTED'::friendship_status, 
                             'DECLINED'::friendship_status, 'BLOCKED'::friendship_status)
    ) THEN
        RAISE EXCEPTION 'Invalid friendship status values found in database';
    END IF;
    
    RAISE NOTICE 'Friendship status validation completed successfully';
END $$;