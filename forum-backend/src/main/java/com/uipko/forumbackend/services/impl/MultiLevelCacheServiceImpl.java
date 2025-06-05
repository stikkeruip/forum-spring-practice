package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uipko.forumbackend.services.MultiLevelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
@Slf4j
public class MultiLevelCacheServiceImpl implements MultiLevelCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // L1 Cache (Local Caffeine Cache)
    private Cache<String, Object> l1Cache;
    
    // Statistics tracking
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l1Misses = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong l2Misses = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong promotions = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    
    // Configuration
    private MultiLevelCacheConfig config = new MultiLevelCacheConfig(
        Duration.ofMinutes(5),  // L1 TTL
        Duration.ofMinutes(30), // L2 TTL
        10000,                  // L1 max size
        true,                   // L1 write-through
        true,                   // L2 write-through
        true,                   // Auto promotion
        true,                   // Async refresh
        0.8,                    // Promotion threshold (80% hit rate)
        Duration.ofMinutes(10)  // Sync interval
    );
    
    public MultiLevelCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        initializeL1Cache();
    }
    
    private void initializeL1Cache() {
        this.l1Cache = Caffeine.newBuilder()
            .maximumSize(config.l1MaxSize())
            .expireAfterWrite(config.l1DefaultTtl())
            .recordStats()
            .removalListener((key, value, cause) -> {
                evictions.incrementAndGet();
                log.debug("L1 cache evicted key: {} (cause: {})", key, cause);
            })
            .build();
        
        log.info("Initialized L1 cache with max size: {} and TTL: {}", 
            config.l1MaxSize(), config.l1DefaultTtl());
    }
    
    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        totalRequests.incrementAndGet();
        
        // Try L1 cache first
        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value != null) {
            l1Hits.incrementAndGet();
            log.debug("L1 cache hit for key: {}", key);
            return Optional.of(type.cast(l1Value));
        }
        
        l1Misses.incrementAndGet();
        
        // Try L2 cache (Redis)
        try {
            Object l2Value = redisTemplate.opsForValue().get(key);
            if (l2Value != null) {
                l2Hits.incrementAndGet();
                log.debug("L2 cache hit for key: {}", key);
                
                T deserializedValue;
                if (l2Value instanceof String) {
                    deserializedValue = objectMapper.readValue((String) l2Value, type);
                } else {
                    deserializedValue = type.cast(l2Value);
                }
                
                // Promote to L1 cache if auto-promotion is enabled
                if (config.enableAutoPromotion()) {
                    l1Cache.put(key, deserializedValue);
                    promotions.incrementAndGet();
                    log.debug("Promoted key to L1 cache: {}", key);
                }
                
                return Optional.of(deserializedValue);
            }
        } catch (Exception e) {
            log.error("Error reading from L2 cache for key: {}", key, e);
        }
        
        l2Misses.incrementAndGet();
        log.debug("Cache miss for key: {}", key);
        return Optional.empty();
    }
    
    @Override
    public <T> T getOrCompute(String key, Class<T> type, Supplier<T> valueSupplier) {
        Optional<T> cachedValue = get(key, type);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        
        // Compute value from supplier
        T computedValue = valueSupplier.get();
        if (computedValue != null) {
            put(key, computedValue);
        }
        
        return computedValue;
    }
    
    @Override
    @Async
    public <T> CompletableFuture<T> getOrComputeAsync(String key, Class<T> type, Supplier<T> valueSupplier) {
        return CompletableFuture.supplyAsync(() -> getOrCompute(key, type, valueSupplier));
    }
    
    @Override
    public <T> void put(String key, T value) {
        put(key, value, config.l2DefaultTtl());
    }
    
    @Override
    public <T> void put(String key, T value, Duration ttl) {
        if (value == null) {
            log.warn("Attempted to cache null value for key: {}", key);
            return;
        }
        
        try {
            // Store in L1 cache
            l1Cache.put(key, value);
            
            // Store in L2 cache (Redis) if write-through is enabled
            if (config.enableL2WriteThrough()) {
                String serializedValue = objectMapper.writeValueAsString(value);
                redisTemplate.opsForValue().set(key, serializedValue, ttl);
            }
            
            log.debug("Cached value for key: {} in both levels", key);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for L2 cache, key: {}", key, e);
            // Still store in L1 even if L2 fails
            l1Cache.put(key, value);
        }
    }
    
    @Override
    public <T> void putL1Only(String key, T value) {
        if (value != null) {
            l1Cache.put(key, value);
            log.debug("Cached value in L1 only for key: {}", key);
        }
    }
    
    @Override
    public <T> void putL2Only(String key, T value, Duration ttl) {
        if (value == null) {
            return;
        }
        
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, serializedValue, ttl);
            log.debug("Cached value in L2 only for key: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Error caching value in L2 only for key: {}", key, e);
        }
    }
    
    @Override
    public void evict(String key) {
        evictL1(key);
        evictL2(key);
        log.debug("Evicted key from both cache levels: {}", key);
    }
    
    @Override
    public void evictL1(String key) {
        l1Cache.invalidate(key);
        log.debug("Evicted key from L1 cache: {}", key);
    }
    
    @Override
    public void evictL2(String key) {
        redisTemplate.delete(key);
        log.debug("Evicted key from L2 cache: {}", key);
    }
    
    @Override
    public void clear() {
        clearL1();
        clearL2();
        log.info("Cleared both cache levels");
    }
    
    @Override
    public void clearL1() {
        l1Cache.invalidateAll();
        log.info("Cleared L1 cache");
    }
    
    @Override
    public void clearL2() {
        // Note: This clears ALL Redis keys, use with caution
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        log.warn("Cleared L2 cache (Redis) - ALL keys removed");
    }
    
    @Override
    public boolean containsKey(String key) {
        return containsKeyL1(key) || containsKeyL2(key);
    }
    
    @Override
    public boolean containsKeyL1(String key) {
        return l1Cache.getIfPresent(key) != null;
    }
    
    @Override
    public boolean containsKeyL2(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    @Override
    public <T> void promote(String key, Class<T> type) {
        if (!containsKeyL1(key) && containsKeyL2(key)) {
            Optional<T> value = get(key, type);
            if (value.isPresent()) {
                l1Cache.put(key, value.get());
                promotions.incrementAndGet();
                log.debug("Manually promoted key to L1: {}", key);
            }
        }
    }
    
    @Override
    public <T> void sync(String key, Class<T> type) {
        try {
            Object l2Value = redisTemplate.opsForValue().get(key);
            if (l2Value != null) {
                T deserializedValue;
                if (l2Value instanceof String) {
                    deserializedValue = objectMapper.readValue((String) l2Value, type);
                } else {
                    deserializedValue = type.cast(l2Value);
                }
                l1Cache.put(key, deserializedValue);
                log.debug("Synced key from L2 to L1: {}", key);
            } else {
                l1Cache.invalidate(key);
                log.debug("Removed key from L1 as it doesn't exist in L2: {}", key);
            }
        } catch (Exception e) {
            log.error("Error syncing cache for key: {}", key, e);
        }
    }
    
    @Override
    public MultiLevelCacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats l1Stats = l1Cache.stats();
        
        long totalReq = totalRequests.get();
        double l1HitRate = totalReq > 0 ? (double) l1Hits.get() / totalReq : 0.0;
        double l2HitRate = totalReq > 0 ? (double) l2Hits.get() / totalReq : 0.0;
        double overallHitRate = totalReq > 0 ? (double) (l1Hits.get() + l2Hits.get()) / totalReq : 0.0;
        
        return new MultiLevelCacheStats(
            l1Hits.get(),
            l1Misses.get(),
            l2Hits.get(),
            l2Misses.get(),
            totalReq,
            l1HitRate,
            l2HitRate,
            overallHitRate,
            l1Cache.estimatedSize(),
            -1, // L2 size would need additional Redis call
            promotions.get(),
            evictions.get()
        );
    }
    
    @Override
    public void configure(MultiLevelCacheConfig newConfig) {
        this.config = newConfig;
        
        // Reinitialize L1 cache with new configuration
        initializeL1Cache();
        
        log.info("Multi-level cache reconfigured: {}", newConfig);
    }
    
    /**
     * Get current configuration
     */
    public MultiLevelCacheConfig getConfig() {
        return config;
    }
    
    /**
     * Manual cache warming method
     */
    public <T> void warmUp(String key, T value, Duration ttl) {
        put(key, value, ttl);
        log.debug("Warmed up cache for key: {}", key);
    }
    
    /**
     * Batch eviction for multiple keys
     */
    public void evictAll(Iterable<String> keys) {
        for (String key : keys) {
            evict(key);
        }
        log.debug("Batch evicted multiple keys");
    }
    
    /**
     * Get detailed cache info for monitoring
     */
    public String getCacheInfo() {
        MultiLevelCacheStats stats = getStats();
        return String.format(
            "MultiLevelCache[L1: %d/%d hits/total, L2: %d/%d hits/total, Overall: %.2f%% hit rate, L1 size: %d]",
            stats.l1Hits(), stats.totalRequests(),
            stats.l2Hits(), stats.totalRequests(),
            stats.overallHitRate() * 100,
            stats.l1Size()
        );
    }
}