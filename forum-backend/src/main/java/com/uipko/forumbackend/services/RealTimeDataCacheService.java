package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.dto.cache.CacheableNotificationDto;
import com.uipko.forumbackend.domain.dto.cache.CacheablePostDto;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.domain.entities.Post;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for caching real-time data for live components
 * Optimizes performance for frequently updated and accessed data
 */
public interface RealTimeDataCacheService {
    
    // Online Users Management
    
    /**
     * Cache online users list with fast lookup
     */
    void cacheOnlineUsers(Set<String> onlineUsers);
    
    /**
     * Add user to online cache
     */
    void addOnlineUser(String username);
    
    /**
     * Remove user from online cache
     */
    void removeOnlineUser(String username);
    
    /**
     * Get cached online users
     */
    Set<String> getCachedOnlineUsers();
    
    /**
     * Check if user is online (cached)
     */
    boolean isUserOnlineCached(String username);
    
    /**
     * Get online user count
     */
    long getOnlineUserCount();
    
    // Live Notifications Management
    
    /**
     * Cache user's recent notifications
     */
    void cacheUserNotifications(String username, List<Notification> notifications);
    
    /**
     * Cache user's recent notifications (DTO version for performance)
     */
    void cacheUserNotificationDtos(String username, List<CacheableNotificationDto> notifications);
    
    /**
     * Add notification to user's cache
     */
    void addNotificationToCache(String username, Notification notification);
    
    /**
     * Get cached notifications for user
     */
    List<Notification> getCachedUserNotifications(String username);
    
    /**
     * Mark notification as read in cache
     */
    void markNotificationAsReadInCache(String username, Long notificationId);
    
    /**
     * Get unread notification count for user
     */
    long getUnreadNotificationCount(String username);
    
    /**
     * Clear all notifications cache for user
     */
    void clearUserNotificationsCache(String username);
    
    // Live Posts Management
    
    /**
     * Cache recent posts for homepage
     */
    void cacheRecentPosts(List<Post> posts);
    
    /**
     * Cache recent posts for homepage (DTO version for performance)
     */
    void cacheRecentPostDtos(List<CacheablePostDto> posts);
    
    /**
     * Add new post to recent posts cache
     */
    void addPostToRecentCache(Post post);
    
    /**
     * Update post in cache
     */
    void updatePostInCache(Post post);
    
    /**
     * Remove post from cache
     */
    void removePostFromCache(Long postId);
    
    /**
     * Get cached recent posts
     */
    List<Post> getCachedRecentPosts();
    
    /**
     * Cache trending posts
     */
    void cacheTrendingPosts(List<Post> posts);
    
    /**
     * Cache trending posts (DTO version for performance)
     */
    void cacheTrendingPostDtos(List<CacheablePostDto> posts);
    
    /**
     * Get cached trending posts
     */
    List<Post> getCachedTrendingPosts();
    
    // Live Activity Feed
    
    /**
     * Cache user activity feed
     */
    void cacheUserActivityFeed(String username, List<ActivityEvent> activities);
    
    /**
     * Add activity to user's feed cache
     */
    void addActivityToFeed(String username, ActivityEvent activity);
    
    /**
     * Get cached activity feed for user
     */
    List<ActivityEvent> getCachedUserActivityFeed(String username);
    
    /**
     * Cache global activity feed
     */
    void cacheGlobalActivityFeed(List<ActivityEvent> activities);
    
    /**
     * Get cached global activity feed
     */
    List<ActivityEvent> getCachedGlobalActivityFeed();
    
    // Live Reactions Cache
    
    /**
     * Cache post reaction counts
     */
    void cachePostReactions(Long postId, Map<String, Long> reactionCounts);
    
    /**
     * Update post reaction in cache
     */
    void updatePostReactionInCache(Long postId, String reactionType, Long newCount);
    
    /**
     * Get cached post reactions
     */
    Map<String, Long> getCachedPostReactions(Long postId);
    
    /**
     * Cache comment reaction counts
     */
    void cacheCommentReactions(Long commentId, Map<String, Long> reactionCounts);
    
    /**
     * Get cached comment reactions
     */
    Map<String, Long> getCachedCommentReactions(Long commentId);
    
    // Live Friend Status Cache
    
    /**
     * Cache user's friends list
     */
    void cacheUserFriends(String username, Set<String> friends);
    
    /**
     * Add friend to user's cache
     */
    void addFriendToCache(String username, String friendUsername);
    
    /**
     * Remove friend from user's cache
     */
    void removeFriendFromCache(String username, String friendUsername);
    
    /**
     * Get cached friends for user
     */
    Set<String> getCachedUserFriends(String username);
    
    /**
     * Cache pending friend requests
     */
    void cachePendingFriendRequests(String username, Set<String> pendingRequests);
    
    /**
     * Get cached pending friend requests
     */
    Set<String> getCachedPendingFriendRequests(String username);
    
    // Cache Warming and Management
    
    /**
     * Warm up all real-time caches
     */
    void warmUpRealTimeCaches();
    
    /**
     * Refresh specific cache
     */
    void refreshCache(String cacheType);
    
    /**
     * Clear all real-time caches
     */
    void clearAllRealTimeCaches();
    
    /**
     * Get cache statistics
     */
    RealTimeCacheStats getCacheStats();
    
    /**
     * Set cache expiration for specific data
     */
    void setCacheExpiration(String cacheKey, Duration expiration);
    
    // Data Models
    
    /**
     * Activity event for activity feeds
     */
    record ActivityEvent(
        String eventId,
        String username,
        String activityType, // POST_CREATED, COMMENT_ADDED, FRIEND_ADDED, etc.
        String targetId,
        String message,
        String timestamp,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Real-time cache statistics
     */
    record RealTimeCacheStats(
        long onlineUsersCount,
        long totalNotificationsCached,
        long recentPostsCount,
        long trendingPostsCount,
        long totalActivitiesCached,
        long totalReactionsCached,
        double averageResponseTime,
        long cacheHitRate
    ) {}
}