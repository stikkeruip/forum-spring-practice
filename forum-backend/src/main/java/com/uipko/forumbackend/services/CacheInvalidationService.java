package com.uipko.forumbackend.services;

import java.util.List;
import java.util.Set;

/**
 * Service for managing cache invalidation patterns to ensure data consistency
 * Provides intelligent cache invalidation based on data relationships
 */
public interface CacheInvalidationService {
    
    // User-related invalidations
    
    /**
     * Invalidate all caches related to a user
     */
    void invalidateUserCaches(String username);
    
    /**
     * Invalidate user authentication caches
     */
    void invalidateUserAuthCache(String username);
    
    /**
     * Invalidate user profile caches
     */
    void invalidateUserProfileCache(String username);
    
    /**
     * Invalidate user's notification caches
     */
    void invalidateUserNotificationCache(String username);
    
    /**
     * Invalidate user's friend caches
     */
    void invalidateUserFriendCache(String username);
    
    // Post-related invalidations
    
    /**
     * Invalidate caches related to a specific post
     */
    void invalidatePostCaches(Long postId);
    
    /**
     * Invalidate recent posts cache
     */
    void invalidateRecentPostsCache();
    
    /**
     * Invalidate trending posts cache
     */
    void invalidateTrendingPostsCache();
    
    /**
     * Invalidate post reaction caches
     */
    void invalidatePostReactionCache(Long postId);
    
    /**
     * Invalidate caches when post is created by user
     */
    void invalidateOnPostCreated(Long postId, String authorUsername);
    
    /**
     * Invalidate caches when post is updated
     */
    void invalidateOnPostUpdated(Long postId, String authorUsername);
    
    /**
     * Invalidate caches when post is deleted
     */
    void invalidateOnPostDeleted(Long postId, String authorUsername);
    
    // Comment-related invalidations
    
    /**
     * Invalidate caches related to a specific comment
     */
    void invalidateCommentCaches(Long commentId);
    
    /**
     * Invalidate comment reaction caches
     */
    void invalidateCommentReactionCache(Long commentId);
    
    /**
     * Invalidate caches when comment is added to post
     */
    void invalidateOnCommentAdded(Long commentId, Long postId, String authorUsername);
    
    /**
     * Invalidate caches when comment is updated
     */
    void invalidateOnCommentUpdated(Long commentId, Long postId, String authorUsername);
    
    /**
     * Invalidate caches when comment is deleted
     */
    void invalidateOnCommentDeleted(Long commentId, Long postId, String authorUsername);
    
    // Reaction-related invalidations
    
    /**
     * Invalidate reaction caches when reaction is added/updated
     */
    void invalidateOnReactionChanged(String reactionType, Long targetId, String targetType, String username);
    
    // Friend-related invalidations
    
    /**
     * Invalidate friend caches for both users
     */
    void invalidateFriendCaches(String username1, String username2);
    
    /**
     * Invalidate caches when friend request is sent
     */
    void invalidateOnFriendRequestSent(String senderUsername, String receiverUsername);
    
    /**
     * Invalidate caches when friend request is accepted
     */
    void invalidateOnFriendRequestAccepted(String username1, String username2);
    
    /**
     * Invalidate caches when friendship is removed
     */
    void invalidateOnFriendshipRemoved(String username1, String username2);
    
    // Notification-related invalidations
    
    /**
     * Invalidate notification caches when new notification is created
     */
    void invalidateOnNotificationCreated(String recipientUsername);
    
    /**
     * Invalidate notification caches when notifications are marked as read
     */
    void invalidateOnNotificationsRead(String username, List<Long> notificationIds);
    
    // Online status invalidations
    
    /**
     * Invalidate online status caches
     */
    void invalidateOnlineStatusCache();
    
    /**
     * Invalidate caches when user comes online
     */
    void invalidateOnUserOnline(String username);
    
    /**
     * Invalidate caches when user goes offline
     */
    void invalidateOnUserOffline(String username);
    
    // Bulk operations
    
    /**
     * Invalidate all caches (use with caution)
     */
    void invalidateAllCaches();
    
    /**
     * Invalidate caches by pattern
     */
    void invalidateCachesByPattern(String pattern);
    
    /**
     * Invalidate specific cache keys
     */
    void invalidateSpecificKeys(Set<String> keys);
    
    // Smart invalidation
    
    /**
     * Intelligently invalidate related caches based on data relationships
     */
    void smartInvalidateRelatedCaches(String entityType, Object entityId, String operation);
    
    // Statistics and monitoring
    
    /**
     * Get cache invalidation statistics
     */
    CacheInvalidationStats getInvalidationStats();
    
    /**
     * Configure invalidation strategies
     */
    void configureInvalidationStrategy(InvalidationStrategy strategy);
    
    /**
     * Cache invalidation statistics
     */
    record CacheInvalidationStats(
        long totalInvalidations,
        long userCacheInvalidations,
        long postCacheInvalidations,
        long notificationCacheInvalidations,
        long friendCacheInvalidations,
        long reactionCacheInvalidations,
        long bulkInvalidations,
        double averageInvalidationTimeMs
    ) {}
    
    /**
     * Invalidation strategy configuration
     */
    record InvalidationStrategy(
        boolean enableSmartInvalidation,
        boolean enableBulkInvalidation,
        boolean enableCascadeInvalidation,
        int maxKeysPerBatch,
        boolean logInvalidations
    ) {}
}