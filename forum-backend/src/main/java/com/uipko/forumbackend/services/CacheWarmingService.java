package com.uipko.forumbackend.services;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for warming up caches with frequently accessed data
 * Improves application performance by preloading data into cache
 */
public interface CacheWarmingService {
    
    /**
     * Warm up all caches during application startup
     */
    CompletableFuture<Void> warmUpAllCaches();
    
    /**
     * Warm up authentication-related caches
     */
    CompletableFuture<Void> warmUpAuthenticationCache();
    
    /**
     * Warm up post-related caches
     */
    CompletableFuture<Void> warmUpPostCache();
    
    /**
     * Warm up user-related caches
     */
    CompletableFuture<Void> warmUpUserCache();
    
    /**
     * Warm up notification caches for active users
     */
    CompletableFuture<Void> warmUpNotificationCache();
    
    /**
     * Warm up friend relationship caches
     */
    CompletableFuture<Void> warmUpFriendCache();
    
    /**
     * Warm up reaction caches for trending content
     */
    CompletableFuture<Void> warmUpReactionCache();
    
    /**
     * Schedule periodic cache warming
     */
    void schedulePeriodicWarmUp();
    
    /**
     * Warm up cache for specific user
     */
    CompletableFuture<Void> warmUpUserSpecificCache(String username);
    
    /**
     * Get cache warming statistics
     */
    CacheWarmingStats getWarmingStats();
    
    /**
     * Configure cache warming strategy
     */
    void configureCacheWarmingStrategy(CacheWarmingConfig config);
    
    /**
     * Cache warming configuration
     */
    record CacheWarmingConfig(
        boolean enableAuthCache,
        boolean enablePostCache,
        boolean enableUserCache,
        boolean enableNotificationCache,
        boolean enableFriendCache,
        boolean enableReactionCache,
        int maxPostsToCache,
        int maxUsersToCache,
        int maxNotificationsPerUser,
        Duration warmUpInterval,
        List<String> priorityUsers
    ) {}
    
    /**
     * Cache warming statistics
     */
    record CacheWarmingStats(
        long totalWarmUps,
        long successfulWarmUps,
        long failedWarmUps,
        long totalItemsWarmed,
        long averageWarmUpTimeMs,
        String lastWarmUpTime,
        String nextScheduledWarmUp
    ) {}
}