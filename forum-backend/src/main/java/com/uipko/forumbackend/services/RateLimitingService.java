package com.uipko.forumbackend.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitingService {
    
    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    public RateLimitingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if the user can make a getFriends request (max 30 per minute)
     */
    public boolean canMakeGetFriendsRequest(String username) {
        return canMakeRequest(username, "getFriends", 30, Duration.ofMinutes(1));
    }
    
    /**
     * Check if the user can make a friendship status request (max 60 per minute)
     */
    public boolean canMakeFriendshipStatusRequest(String username) {
        return canMakeRequest(username, "friendshipStatus", 60, Duration.ofMinutes(1));
    }
    
    /**
     * Check if the user can make general friend requests (max 30 per minute)
     */
    public boolean canMakeGeneralRequest(String username) {
        return canMakeRequest(username, "general", 30, Duration.ofMinutes(1));
    }
    
    /**
     * Check if the user can make post creation requests (max 10 per minute)
     */
    public boolean canMakePostRequest(String username) {
        return canMakeRequest(username, "createPost", 10, Duration.ofMinutes(1));
    }
    
    /**
     * Check if the user can make comment requests (max 20 per minute)
     */
    public boolean canMakeCommentRequest(String username) {
        return canMakeRequest(username, "createComment", 20, Duration.ofMinutes(1));
    }
    
    /**
     * Check if the user can make reaction requests (max 100 per minute)
     */
    public boolean canMakeReactionRequest(String username) {
        return canMakeRequest(username, "reaction", 100, Duration.ofMinutes(1));
    }
    
    /**
     * Generic Redis-based sliding window rate limiting
     */
    public boolean canMakeRequest(String username, String operation, int maxRequests, Duration windowDuration) {
        if (username == null || operation == null) {
            return false;
        }
        
        try {
            // Use Redis for distributed rate limiting with sliding window
            return isAllowedBySlidingWindow(username, operation, maxRequests, windowDuration);
        } catch (Exception e) {
            log.warn("Redis rate limiting failed for user {}, operation {}: {}. Falling back to local bucket.", 
                    username, operation, e.getMessage());
            // Fallback to local bucket4j implementation
            return getOrCreateBucket(username, operation, maxRequests, windowDuration).tryConsume(1);
        }
    }
    
    /**
     * Redis sliding window algorithm for distributed rate limiting
     */
    private boolean isAllowedBySlidingWindow(String username, String operation, int maxRequests, Duration windowDuration) {
        String key = RATE_LIMIT_PREFIX + username + ":" + operation;
        long now = System.currentTimeMillis();
        long windowStart = now - windowDuration.toMillis();
        
        // Remove old entries (sliding window cleanup)
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // Count current requests in window
        Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, now);
        
        if (currentCount != null && currentCount >= maxRequests) {
            log.debug("Rate limit exceeded for user {}, operation {}: {}/{}", 
                    username, operation, currentCount, maxRequests);
            return false;
        }
        
        // Add current request timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        
        // Set expiration to prevent memory leaks
        redisTemplate.expire(key, windowDuration.plusMinutes(1));
        
        log.debug("Rate limit check passed for user {}, operation {}: {}/{}", 
                username, operation, (currentCount != null ? currentCount + 1 : 1), maxRequests);
        return true;
    }
    
    private Bucket getOrCreateBucket(String username, String operation, int capacity, Duration refillPeriod) {
        String key = username + ":" + operation;
        return cache.computeIfAbsent(key, k -> createNewBucket(capacity, refillPeriod));
    }
    
    private Bucket createNewBucket(int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillPeriod));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}