package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.services.AuthenticationCacheService;
import com.uipko.forumbackend.services.CacheInvalidationService;
import com.uipko.forumbackend.services.RealTimeDataCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class CacheInvalidationServiceImpl implements CacheInvalidationService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;
    private final AuthenticationCacheService authCacheService;
    private final RealTimeDataCacheService realTimeCacheService;
    
    // Statistics tracking
    private final AtomicLong totalInvalidations = new AtomicLong(0);
    private final AtomicLong userCacheInvalidations = new AtomicLong(0);
    private final AtomicLong postCacheInvalidations = new AtomicLong(0);
    private final AtomicLong notificationCacheInvalidations = new AtomicLong(0);
    private final AtomicLong friendCacheInvalidations = new AtomicLong(0);
    private final AtomicLong reactionCacheInvalidations = new AtomicLong(0);
    private final AtomicLong bulkInvalidations = new AtomicLong(0);
    
    private InvalidationStrategy strategy = new InvalidationStrategy(
        true, true, true, 100, true
    );
    
    public CacheInvalidationServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                      CacheManager cacheManager,
                                      AuthenticationCacheService authCacheService,
                                      RealTimeDataCacheService realTimeCacheService) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
        this.authCacheService = authCacheService;
        this.realTimeCacheService = realTimeCacheService;
    }
    
    // User-related invalidations
    
    @Override
    public void invalidateUserCaches(String username) {
        log.debug("Invalidating all caches for user: {}", username);
        
        invalidateUserAuthCache(username);
        invalidateUserProfileCache(username);
        invalidateUserNotificationCache(username);
        invalidateUserFriendCache(username);
        
        userCacheInvalidations.incrementAndGet();
        totalInvalidations.incrementAndGet();
        
        if (strategy.logInvalidations()) {
            log.info("Invalidated all caches for user: {}", username);
        }
    }
    
    @Override
    public void invalidateUserAuthCache(String username) {
        authCacheService.invalidateUserAuthCache(username);
        
        // Invalidate Spring Cache entries
        if (cacheManager.getCache("users") != null) {
            cacheManager.getCache("users").evict(username);
        }
        
        log.debug("Invalidated auth cache for user: {}", username);
    }
    
    @Override
    public void invalidateUserProfileCache(String username) {
        // Invalidate user entity cache
        Set<String> userKeys = redisTemplate.keys("forum:auth:user:" + username);
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
        
        // Invalidate Spring Cache entries
        if (cacheManager.getCache("users") != null) {
            cacheManager.getCache("users").evict(username);
        }
        
        log.debug("Invalidated profile cache for user: {}", username);
    }
    
    @Override
    public void invalidateUserNotificationCache(String username) {
        realTimeCacheService.clearUserNotificationsCache(username);
        
        // Invalidate Spring Cache entries
        if (cacheManager.getCache("notifications") != null) {
            cacheManager.getCache("notifications").evict(username);
        }
        
        notificationCacheInvalidations.incrementAndGet();
        log.debug("Invalidated notification cache for user: {}", username);
    }
    
    @Override
    public void invalidateUserFriendCache(String username) {
        // Invalidate friend-related caches
        Set<String> friendKeys = redisTemplate.keys("forum:realtime:friends:" + username);
        Set<String> pendingKeys = redisTemplate.keys("forum:realtime:pending:" + username);
        
        if (friendKeys != null && !friendKeys.isEmpty()) {
            redisTemplate.delete(friendKeys);
        }
        if (pendingKeys != null && !pendingKeys.isEmpty()) {
            redisTemplate.delete(pendingKeys);
        }
        
        // Invalidate Spring Cache entries
        if (cacheManager.getCache("friends") != null) {
            cacheManager.getCache("friends").evict(username);
        }
        
        friendCacheInvalidations.incrementAndGet();
        log.debug("Invalidated friend cache for user: {}", username);
    }
    
    // Post-related invalidations
    
    @Override
    public void invalidatePostCaches(Long postId) {
        log.debug("Invalidating caches for post: {}", postId);
        
        // Invalidate specific post cache
        if (cacheManager.getCache("posts") != null) {
            cacheManager.getCache("posts").evict(postId);
        }
        
        // Invalidate post reactions
        invalidatePostReactionCache(postId);
        
        // Invalidate related caches
        invalidateRecentPostsCache();
        invalidateTrendingPostsCache();
        
        postCacheInvalidations.incrementAndGet();
        totalInvalidations.incrementAndGet();
    }
    
    @Override
    public void invalidateRecentPostsCache() {
        redisTemplate.delete("forum:realtime:posts:recent");
        log.debug("Invalidated recent posts cache");
    }
    
    @Override
    public void invalidateTrendingPostsCache() {
        redisTemplate.delete("forum:realtime:posts:trending");
        log.debug("Invalidated trending posts cache");
    }
    
    @Override
    public void invalidatePostReactionCache(Long postId) {
        redisTemplate.delete("forum:realtime:reactions:post:" + postId);
        reactionCacheInvalidations.incrementAndGet();
        log.debug("Invalidated reaction cache for post: {}", postId);
    }
    
    @Override
    public void invalidateOnPostCreated(Long postId, String authorUsername) {
        // Invalidate recent posts (new post should appear)
        invalidateRecentPostsCache();
        
        // Potentially invalidate trending posts
        invalidateTrendingPostsCache();
        
        // Invalidate author's activity feed
        Set<String> activityKeys = redisTemplate.keys("forum:realtime:activity:user:" + authorUsername);
        if (activityKeys != null && !activityKeys.isEmpty()) {
            redisTemplate.delete(activityKeys);
        }
        
        // Invalidate global activity feed
        redisTemplate.delete("forum:realtime:activity:global");
        
        if (strategy.enableSmartInvalidation()) {
            smartInvalidateRelatedCaches("POST", postId, "CREATE");
        }
        
        log.debug("Invalidated caches for post creation: {} by {}", postId, authorUsername);
    }
    
    @Override
    public void invalidateOnPostUpdated(Long postId, String authorUsername) {
        invalidatePostCaches(postId);
        
        if (strategy.enableSmartInvalidation()) {
            smartInvalidateRelatedCaches("POST", postId, "UPDATE");
        }
        
        log.debug("Invalidated caches for post update: {} by {}", postId, authorUsername);
    }
    
    @Override
    public void invalidateOnPostDeleted(Long postId, String authorUsername) {
        invalidatePostCaches(postId);
        
        // Remove from recent and trending posts
        invalidateRecentPostsCache();
        invalidateTrendingPostsCache();
        
        if (strategy.enableSmartInvalidation()) {
            smartInvalidateRelatedCaches("POST", postId, "DELETE");
        }
        
        log.debug("Invalidated caches for post deletion: {} by {}", postId, authorUsername);
    }
    
    // Comment-related invalidations
    
    @Override
    public void invalidateCommentCaches(Long commentId) {
        // Invalidate comment reactions
        invalidateCommentReactionCache(commentId);
        
        log.debug("Invalidated caches for comment: {}", commentId);
    }
    
    @Override
    public void invalidateCommentReactionCache(Long commentId) {
        redisTemplate.delete("forum:realtime:reactions:comment:" + commentId);
        reactionCacheInvalidations.incrementAndGet();
        log.debug("Invalidated reaction cache for comment: {}", commentId);
    }
    
    @Override
    public void invalidateOnCommentAdded(Long commentId, Long postId, String authorUsername) {
        // Invalidate the parent post cache (comment count may have changed)
        invalidatePostCaches(postId);
        
        // Invalidate recent posts cache (post activity changed)
        invalidateRecentPostsCache();
        
        // Invalidate trending posts (activity affects trending)
        invalidateTrendingPostsCache();
        
        log.debug("Invalidated caches for comment addition: {} on post {} by {}", commentId, postId, authorUsername);
    }
    
    @Override
    public void invalidateOnCommentUpdated(Long commentId, Long postId, String authorUsername) {
        invalidateCommentCaches(commentId);
        
        log.debug("Invalidated caches for comment update: {} on post {} by {}", commentId, postId, authorUsername);
    }
    
    @Override
    public void invalidateOnCommentDeleted(Long commentId, Long postId, String authorUsername) {
        invalidateCommentCaches(commentId);
        
        // Invalidate parent post cache (comment count changed)
        invalidatePostCaches(postId);
        
        log.debug("Invalidated caches for comment deletion: {} on post {} by {}", commentId, postId, authorUsername);
    }
    
    // Reaction-related invalidations
    
    @Override
    public void invalidateOnReactionChanged(String reactionType, Long targetId, String targetType, String username) {
        if ("POST".equals(targetType)) {
            invalidatePostReactionCache(targetId);
            // Post reaction change might affect trending
            invalidateTrendingPostsCache();
        } else if ("COMMENT".equals(targetType)) {
            invalidateCommentReactionCache(targetId);
        }
        
        reactionCacheInvalidations.incrementAndGet();
        log.debug("Invalidated reaction cache for {} {} by {}: {}", targetType, targetId, username, reactionType);
    }
    
    // Friend-related invalidations
    
    @Override
    public void invalidateFriendCaches(String username1, String username2) {
        invalidateUserFriendCache(username1);
        invalidateUserFriendCache(username2);
        
        friendCacheInvalidations.addAndGet(2);
        log.debug("Invalidated friend caches for users: {} and {}", username1, username2);
    }
    
    @Override
    public void invalidateOnFriendRequestSent(String senderUsername, String receiverUsername) {
        // Invalidate pending requests cache for receiver
        Set<String> pendingKeys = redisTemplate.keys("forum:realtime:pending:" + receiverUsername);
        if (pendingKeys != null && !pendingKeys.isEmpty()) {
            redisTemplate.delete(pendingKeys);
        }
        
        log.debug("Invalidated caches for friend request: {} -> {}", senderUsername, receiverUsername);
    }
    
    @Override
    public void invalidateOnFriendRequestAccepted(String username1, String username2) {
        invalidateFriendCaches(username1, username2);
        
        // Clear pending requests
        Set<String> pendingKeys1 = redisTemplate.keys("forum:realtime:pending:" + username1);
        Set<String> pendingKeys2 = redisTemplate.keys("forum:realtime:pending:" + username2);
        
        if (pendingKeys1 != null && !pendingKeys1.isEmpty()) {
            redisTemplate.delete(pendingKeys1);
        }
        if (pendingKeys2 != null && !pendingKeys2.isEmpty()) {
            redisTemplate.delete(pendingKeys2);
        }
        
        log.debug("Invalidated caches for friend request acceptance: {} <-> {}", username1, username2);
    }
    
    @Override
    public void invalidateOnFriendshipRemoved(String username1, String username2) {
        invalidateFriendCaches(username1, username2);
        
        log.debug("Invalidated caches for friendship removal: {} <-> {}", username1, username2);
    }
    
    // Notification-related invalidations
    
    @Override
    public void invalidateOnNotificationCreated(String recipientUsername) {
        invalidateUserNotificationCache(recipientUsername);
        
        log.debug("Invalidated notification cache for new notification to: {}", recipientUsername);
    }
    
    @Override
    public void invalidateOnNotificationsRead(String username, List<Long> notificationIds) {
        invalidateUserNotificationCache(username);
        
        log.debug("Invalidated notification cache for read notifications by: {}", username);
    }
    
    // Online status invalidations
    
    @Override
    public void invalidateOnlineStatusCache() {
        redisTemplate.delete("forum:realtime:online_users");
        log.debug("Invalidated online status cache");
    }
    
    @Override
    public void invalidateOnUserOnline(String username) {
        invalidateOnlineStatusCache();
        
        // Update user profile cache
        invalidateUserProfileCache(username);
        
        log.debug("Invalidated caches for user coming online: {}", username);
    }
    
    @Override
    public void invalidateOnUserOffline(String username) {
        invalidateOnlineStatusCache();
        
        // Update user profile cache
        invalidateUserProfileCache(username);
        
        log.debug("Invalidated caches for user going offline: {}", username);
    }
    
    // Bulk operations
    
    @Override
    public void invalidateAllCaches() {
        log.warn("Invalidating ALL caches - this is expensive!");
        
        // Clear all Redis caches
        Set<String> allKeys = redisTemplate.keys("forum:*");
        if (allKeys != null && !allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
        }
        
        // Clear all Spring caches
        cacheManager.getCacheNames().forEach(cacheName -> {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
            }
        });
        
        bulkInvalidations.incrementAndGet();
        totalInvalidations.addAndGet(allKeys != null ? allKeys.size() : 0);
        
        log.warn("Invalidated ALL caches - {} keys cleared", allKeys != null ? allKeys.size() : 0);
    }
    
    @Override
    public void invalidateCachesByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            if (strategy.enableBulkInvalidation() && keys.size() <= strategy.maxKeysPerBatch()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} keys by pattern: {}", keys.size(), pattern);
            } else {
                log.warn("Too many keys ({}) for pattern: {}. Skipping bulk invalidation.", keys.size(), pattern);
            }
        }
        
        bulkInvalidations.incrementAndGet();
        totalInvalidations.addAndGet(keys != null ? keys.size() : 0);
    }
    
    @Override
    public void invalidateSpecificKeys(Set<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            totalInvalidations.addAndGet(keys.size());
            log.debug("Invalidated {} specific keys", keys.size());
        }
    }
    
    // Smart invalidation
    
    @Override
    public void smartInvalidateRelatedCaches(String entityType, Object entityId, String operation) {
        if (!strategy.enableSmartInvalidation()) {
            return;
        }
        
        switch (entityType.toUpperCase()) {
            case "POST":
                handleSmartPostInvalidation((Long) entityId, operation);
                break;
            case "COMMENT":
                handleSmartCommentInvalidation((Long) entityId, operation);
                break;
            case "USER":
                handleSmartUserInvalidation((String) entityId, operation);
                break;
            default:
                log.debug("Unknown entity type for smart invalidation: {}", entityType);
        }
    }
    
    private void handleSmartPostInvalidation(Long postId, String operation) {
        // Smart invalidation based on post relationships
        switch (operation.toUpperCase()) {
            case "CREATE":
                // New post affects recent posts, potentially trending
                invalidateRecentPostsCache();
                break;
            case "UPDATE":
                // Updated post might affect trending status
                invalidateTrendingPostsCache();
                break;
            case "DELETE":
                // Deleted post should be removed from all listings
                invalidateRecentPostsCache();
                invalidateTrendingPostsCache();
                break;
        }
    }
    
    private void handleSmartCommentInvalidation(Long commentId, String operation) {
        // Smart invalidation based on comment relationships
        // Comments affect parent post activity
        invalidateRecentPostsCache();
        invalidateTrendingPostsCache();
    }
    
    private void handleSmartUserInvalidation(String username, String operation) {
        // Smart invalidation based on user relationships
        switch (operation.toUpperCase()) {
            case "UPDATE":
                invalidateUserCaches(username);
                break;
            case "ONLINE":
                invalidateOnUserOnline(username);
                break;
            case "OFFLINE":
                invalidateOnUserOffline(username);
                break;
        }
    }
    
    // Statistics and monitoring
    
    @Override
    public CacheInvalidationStats getInvalidationStats() {
        return new CacheInvalidationStats(
            totalInvalidations.get(),
            userCacheInvalidations.get(),
            postCacheInvalidations.get(),
            notificationCacheInvalidations.get(),
            friendCacheInvalidations.get(),
            reactionCacheInvalidations.get(),
            bulkInvalidations.get(),
            0.0 // Average time would need additional tracking
        );
    }
    
    @Override
    public void configureInvalidationStrategy(InvalidationStrategy newStrategy) {
        this.strategy = newStrategy;
        log.info("Cache invalidation strategy updated: {}", newStrategy);
    }
}