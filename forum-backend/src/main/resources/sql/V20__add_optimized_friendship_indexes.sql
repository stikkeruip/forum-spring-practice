-- Additional optimized indexes for friendship queries based on performance analysis
-- These indexes target the most frequent query patterns identified in FriendshipRepository

-- Index for bidirectional friendship lookups (findFriendshipBetweenUsers)
-- This query searches for friendships in both directions: (user1, user2) OR (user2, user1)
CREATE INDEX idx_friendships_bidirectional_lookup ON friendships(
    LEAST(requester_username, addressee_username), 
    GREATEST(requester_username, addressee_username)
);

-- Index optimized for friendship counts by status (countExistingFriendshipOrRequest)
-- This query checks for any existing friendship between users with specific statuses
CREATE INDEX idx_friendships_users_status ON friendships(
    requester_username, 
    addressee_username, 
    status
);

-- Index for finding all accepted friends with online status join
-- Optimizes findAcceptedFriendsWithOnlineStatus query performance
CREATE INDEX idx_friendships_status_updated_date ON friendships(status, updated_date DESC)
WHERE status = 'ACCEPTED';

-- Index for pending requests ordered by creation date
-- Optimizes getPendingRequests and getSentRequests performance
CREATE INDEX idx_friendships_pending_created ON friendships(status, created_date DESC)
WHERE status = 'PENDING';

-- Covering index for friend list queries to avoid table lookups
-- Includes all columns needed for basic friend information
CREATE INDEX idx_friendships_covering_accepted ON friendships(
    requester_username, 
    addressee_username, 
    status, 
    created_date, 
    updated_date, 
    id
) WHERE status = 'ACCEPTED';

-- Add comments for documentation
COMMENT ON INDEX idx_friendships_bidirectional_lookup IS 'Optimizes bidirectional friendship lookups by normalizing user pair order';
COMMENT ON INDEX idx_friendships_users_status IS 'Optimizes friendship existence checks with specific status filters';
COMMENT ON INDEX idx_friendships_status_updated_date IS 'Optimizes accepted friendships queries with recency ordering';
COMMENT ON INDEX idx_friendships_pending_created IS 'Optimizes pending request queries with chronological ordering';
COMMENT ON INDEX idx_friendships_covering_accepted IS 'Covering index to avoid table lookups for accepted friendships';