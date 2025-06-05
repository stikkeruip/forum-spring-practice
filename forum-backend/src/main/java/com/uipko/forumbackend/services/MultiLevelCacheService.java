package com.uipko.forumbackend.services;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Multi-level caching service with L1 (local) and L2 (Redis) cache layers
 * Provides optimal performance with local caching and distributed consistency with Redis
 */
public interface MultiLevelCacheService {
    
    /**
     * Get value from cache with automatic fallback from L1 -> L2 -> source
     */
    <T> Optional<T> get(String key, Class<T> type);
    
    /**
     * Get value with fallback to supplier if not found in any cache level
     */
    <T> T getOrCompute(String key, Class<T> type, Supplier<T> valueSupplier);
    
    /**
     * Get value asynchronously with fallback to supplier
     */
    <T> CompletableFuture<T> getOrComputeAsync(String key, Class<T> type, Supplier<T> valueSupplier);
    
    /**
     * Put value in both L1 and L2 caches
     */
    <T> void put(String key, T value);
    
    /**
     * Put value with custom TTL
     */
    <T> void put(String key, T value, Duration ttl);
    
    /**
     * Put value only in L1 cache (for frequently accessed, short-lived data)
     */
    <T> void putL1Only(String key, T value);
    
    /**
     * Put value only in L2 cache (for shared, long-lived data)
     */
    <T> void putL2Only(String key, T value, Duration ttl);
    
    /**
     * Evict from both cache levels
     */
    void evict(String key);
    
    /**
     * Evict from L1 cache only
     */
    void evictL1(String key);
    
    /**
     * Evict from L2 cache only
     */
    void evictL2(String key);
    
    /**
     * Clear all entries from both cache levels
     */
    void clear();
    
    /**
     * Clear L1 cache only
     */
    void clearL1();
    
    /**
     * Clear L2 cache only
     */
    void clearL2();
    
    /**
     * Check if key exists in any cache level
     */
    boolean containsKey(String key);
    
    /**
     * Check if key exists in L1 cache
     */
    boolean containsKeyL1(String key);
    
    /**
     * Check if key exists in L2 cache
     */
    boolean containsKeyL2(String key);
    
    /**
     * Promote value from L2 to L1 cache
     */
    <T> void promote(String key, Class<T> type);
    
    /**
     * Sync L1 cache with L2 cache for specific key
     */
    <T> void sync(String key, Class<T> type);
    
    /**
     * Get cache statistics
     */
    MultiLevelCacheStats getStats();
    
    /**
     * Configure cache behavior
     */
    void configure(MultiLevelCacheConfig config);
    
    /**
     * Cache configuration
     */
    record MultiLevelCacheConfig(
        Duration l1DefaultTtl,
        Duration l2DefaultTtl,
        int l1MaxSize,
        boolean enableL1WriteThrough,
        boolean enableL2WriteThrough,
        boolean enableAutoPromotion,
        boolean enableAsyncRefresh,
        double promotionThreshold,
        Duration syncInterval
    ) {}
    
    /**
     * Cache statistics
     */
    record MultiLevelCacheStats(
        long l1Hits,
        long l1Misses,
        long l2Hits,
        long l2Misses,
        long totalRequests,
        double l1HitRate,
        double l2HitRate,
        double overallHitRate,
        long l1Size,
        long l2Size,
        long promotions,
        long evictions
    ) {}
}