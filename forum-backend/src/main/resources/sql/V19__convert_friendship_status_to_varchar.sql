-- Convert friendship_status from custom enum to VARCHAR for better Hibernate compatibility
-- This resolves the "operator does not exist: friendship_status = character varying" error

-- Step 1: Add a temporary VARCHAR column
ALTER TABLE friendships ADD COLUMN status_temp VARCHAR(20);

-- Step 2: Copy data from enum column to varchar column
UPDATE friendships SET status_temp = status::text;

-- Step 3: Drop the enum column
ALTER TABLE friendships DROP COLUMN status;

-- Step 4: Rename the temp column to status
ALTER TABLE friendships RENAME COLUMN status_temp TO status;

-- Step 5: Add NOT NULL constraint and default value
ALTER TABLE friendships ALTER COLUMN status SET NOT NULL;
ALTER TABLE friendships ALTER COLUMN status SET DEFAULT 'PENDING';

-- Step 6: Add check constraint to ensure only valid values
ALTER TABLE friendships ADD CONSTRAINT friendship_status_check 
    CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'BLOCKED'));

-- Step 7: Recreate the index on status column
CREATE INDEX idx_friendships_status_varchar ON friendships(status);

-- Step 8: Recreate compound indexes with varchar status
CREATE INDEX idx_friendships_requester_status_varchar ON friendships(requester_username, status);
CREATE INDEX idx_friendships_addressee_status_varchar ON friendships(addressee_username, status);

-- Step 9: Drop the custom enum type (now unused)
DROP TYPE friendship_status;

-- Add comment for documentation
COMMENT ON COLUMN friendships.status IS 'Friendship status: PENDING, ACCEPTED, DECLINED, BLOCKED (VARCHAR for Hibernate compatibility)';