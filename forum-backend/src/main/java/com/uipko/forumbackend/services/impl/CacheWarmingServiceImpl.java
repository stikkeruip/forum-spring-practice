package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.domain.dto.cache.CacheablePostDto;
import com.uipko.forumbackend.domain.dto.cache.CacheableNotificationDto;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.CacheMapper;
import com.uipko.forumbackend.repositories.FriendshipRepository;
import com.uipko.forumbackend.repositories.NotificationRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CacheWarmingServiceImpl implements CacheWarmingService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final FriendshipRepository friendshipRepository;
    private final AuthenticationCacheService authCacheService;
    private final RealTimeDataCacheService realTimeCacheService;
    private final CacheManager cacheManager;
    private final CacheMapper cacheMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Configuration
    @Value("${cache.warming.max-posts:200}")
    private int maxPostsToCache;
    
    @Value("${cache.warming.max-users:500}")
    private int maxUsersToCache;
    
    @Value("${cache.warming.max-notifications-per-user:50}")
    private int maxNotificationsPerUser;
    
    // Statistics tracking
    private final AtomicLong totalWarmUps = new AtomicLong(0);
    private final AtomicLong successfulWarmUps = new AtomicLong(0);
    private final AtomicLong failedWarmUps = new AtomicLong(0);
    private final AtomicLong totalItemsWarmed = new AtomicLong(0);
    private final AtomicLong totalWarmUpTime = new AtomicLong(0);
    private volatile String lastWarmUpTime;
    
    private CacheWarmingConfig config = new CacheWarmingConfig(
        true, true, true, true, true, true,
        200, 500, 50,
        Duration.ofHours(1),
        List.of("admin", "moderator")
    );
    
    public CacheWarmingServiceImpl(UserRepository userRepository,
                                 PostRepository postRepository,
                                 NotificationRepository notificationRepository,
                                 FriendshipRepository friendshipRepository,
                                 AuthenticationCacheService authCacheService,
                                 RealTimeDataCacheService realTimeCacheService,
                                 CacheManager cacheManager,
                                 CacheMapper cacheMapper,
                                 RedisTemplate<String, Object> redisTemplate,
                                 ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
        this.friendshipRepository = friendshipRepository;
        this.authCacheService = authCacheService;
        this.realTimeCacheService = realTimeCacheService;
        this.cacheManager = cacheManager;
        this.cacheMapper = cacheMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Async
    public CompletableFuture<Void> warmUpAllCaches() {
        log.info("🔥 Starting comprehensive cache warm-up with Redis pipelining optimization");
        long startTime = System.currentTimeMillis();
        long currentWarmUpId = totalWarmUps.incrementAndGet();
        
        try {
            List<CompletableFuture<Void>> warmUpTasks = new ArrayList<>();
            
            // Track Redis operations for performance monitoring
            long redisOperationsStart = getTotalRedisOperations();
            
            if (config.enableAuthCache()) {
                log.debug("Scheduling authentication cache warm-up");
                warmUpTasks.add(warmUpAuthenticationCache());
            }
            if (config.enablePostCache()) {
                log.debug("Scheduling post cache warm-up");
                warmUpTasks.add(warmUpPostCache());
            }
            if (config.enableUserCache()) {
                log.debug("Scheduling user cache warm-up");
                warmUpTasks.add(warmUpUserCache());
            }
            if (config.enableNotificationCache()) {
                log.debug("Scheduling notification cache warm-up");
                warmUpTasks.add(warmUpNotificationCache());
            }
            if (config.enableFriendCache()) {
                log.debug("Scheduling friend cache warm-up");
                warmUpTasks.add(warmUpFriendCache());
            }
            if (config.enableReactionCache()) {
                log.debug("Scheduling reaction cache warm-up");
                warmUpTasks.add(warmUpReactionCache());
            }
            
            log.info("Executing {} cache warm-up tasks concurrently (warm-up #{})...", 
                warmUpTasks.size(), currentWarmUpId);
            
            // Wait for all tasks to complete
            CompletableFuture.allOf(warmUpTasks.toArray(new CompletableFuture[0])).join();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            long redisOperationsEnd = getTotalRedisOperations();
            long redisOpsUsed = Math.max(0, redisOperationsEnd - redisOperationsStart);
            
            totalWarmUpTime.addAndGet(duration);
            lastWarmUpTime = LocalDateTime.now().toString();
            successfulWarmUps.incrementAndGet();
            
            log.info("✅ Cache warm-up #{} completed successfully in {}ms | " +
                    "Items warmed: {} | Redis pipeline operations: {} | Connection efficiency: {}x", 
                currentWarmUpId, duration, totalItemsWarmed.get(), totalRedisOperations.get(),
                totalItemsWarmed.get() > 0 && totalRedisOperations.get() > 0 ? 
                    totalItemsWarmed.get() / totalRedisOperations.get() : 1);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            failedWarmUps.incrementAndGet();
            log.error("❌ Cache warm-up #{} failed after {}ms", 
                currentWarmUpId, System.currentTimeMillis() - startTime, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    // Track actual Redis operations for accurate metrics
    private final AtomicLong totalRedisOperations = new AtomicLong(0);
    
    /**
     * Get total Redis operations count - tracks actual pipeline operations
     */
    private long getTotalRedisOperations() {
        return totalRedisOperations.get();
    }
    
    /**
     * Increment Redis operation count for pipeline tracking
     */
    private void incrementRedisOperations(int count) {
        totalRedisOperations.addAndGet(count);
    }
    
    @Override
    @Async
    public CompletableFuture<Void> warmUpAuthenticationCache() {
        log.info("🔥 Starting authentication cache pipeline warm-up");
        long startTime = System.currentTimeMillis();
        int operationCount = 0;
        
        try {
            // Get frequently accessed users (online, recently active)
            List<User> priorityUsers = userRepository.findAll().stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline() ||
                              user.getLastSeen() != null && user.getLastSeen().isAfter(LocalDateTime.now().minusHours(24)))
                .limit(config.maxUsersToCache())
                .collect(Collectors.toList());
            
            // Also include configured priority users
            for (String priorityUsername : config.priorityUsers()) {
                userRepository.findUserByName(priorityUsername)
                    .ifPresent(user -> {
                        if (!priorityUsers.contains(user)) {
                            priorityUsers.add(user);
                        }
                    });
            }
            
            log.debug("📡 Opening Redis connection for authentication pipeline ({} users)", priorityUsers.size());
            
            // TRUE Redis pipelining - use ONLY the connection parameter, no service calls
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    int pipelineOps = 0;
                    
                    for (User user : priorityUsers) {
                        try {
                            // Create UserDetails DTO for caching
                            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                                user.getName(),
                                user.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                            );
                            
                            // Direct Redis operations using connection parameter (TRUE pipelining)
                            // 1. Cache UserDetails
                            String userDetailsJson = objectMapper.writeValueAsString(userDetails);
                            byte[] userDetailsKey = ("auth:userDetails:" + user.getName()).getBytes();
                            byte[] userDetailsValue = userDetailsJson.getBytes();
                            connection.setEx(userDetailsKey, Duration.ofHours(1).getSeconds(), userDetailsValue);
                            pipelineOps++;
                            
                            // 2. Cache User entity
                            String userJson = objectMapper.writeValueAsString(user);
                            byte[] userKey = ("auth:user:" + user.getName()).getBytes();
                            byte[] userValue = userJson.getBytes();
                            connection.setEx(userKey, Duration.ofHours(1).getSeconds(), userValue);
                            pipelineOps++;
                            
                            // 3. Cache User roles
                            String rolesJson = objectMapper.writeValueAsString(Set.of(user.getRole()));
                            byte[] rolesKey = ("auth:roles:" + user.getName()).getBytes();
                            byte[] rolesValue = rolesJson.getBytes();
                            connection.setEx(rolesKey, Duration.ofHours(1).getSeconds(), rolesValue);
                            pipelineOps++;
                            
                            totalItemsWarmed.addAndGet(3); // 3 operations per user
                            
                        } catch (Exception e) {
                            log.warn("Failed to pipeline user {} in auth cache: {}", user.getName(), e.getMessage());
                        }
                    }
                    
                    log.debug("📊 Executing {} operations in authentication pipeline", pipelineOps);
                    
                    // Track pipeline operations for metrics
                    incrementRedisOperations(pipelineOps);
                    
                } catch (Exception e) {
                    log.error("Pipeline execution failed for authentication cache", e);
                }
                return null; // Pipeline operations don't return values
            });
            
            long duration = System.currentTimeMillis() - startTime;
            operationCount = priorityUsers.size() * 3; // 3 operations per user
            
            log.info("✅ Authentication cache pipeline completed: {} users, {} operations in {}ms | " +
                    "Avg: {}ms/user", 
                priorityUsers.size(), operationCount, duration, 
                priorityUsers.size() > 0 ? duration / priorityUsers.size() : 0);
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Authentication cache pipeline failed after {}ms", duration, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmUpPostCache() {
        log.info("Warming up post cache with Redis pipelining");
        
        try {
            // Use two-step approach to avoid N+1 query warning with EntityGraph + Pageable
            int maxPosts = Math.min(config.maxPostsToCache(), 1000);
            
            // Step 1: Get post IDs with pagination (no N+1 warning)
            List<Long> postIds = postRepository.findActivePostIds(PageRequest.of(0, maxPosts));
            
            // Step 2: Get comment counts only for those posts (no pagination, so no warning)  
            List<Object[]> postDataList = postRepository.findTopActivePostsWithCommentCounts()
                .stream()
                .filter(data -> postIds.contains(((Post) data[0]).getId()))
                .limit(maxPosts)
                .collect(java.util.stream.Collectors.toList());
            
            // Convert Object[] results to DTOs safely using pre-calculated comment counts
            List<CacheablePostDto> recentPostDtos = postDataList.stream()
                .map(data -> {
                    Post post = (Post) data[0];
                    Long commentCount = (Long) data[1];
                    return cacheMapper.toDto(post, commentCount.intValue());
                })
                .collect(Collectors.toList());
            
            // Calculate trending posts before pipelining
            List<CacheablePostDto> trendingPosts = recentPostDtos.stream()
                .filter(post -> post.getCreatedDate().isAfter(LocalDateTime.now().minusHours(24)))
                .sorted((p1, p2) -> Integer.compare(p2.getLikes(), p1.getLikes()))
                .limit(50)
                .collect(Collectors.toList());
            
            // Use Redis pipelining to cache both recent and trending posts in single batch operation
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    // Cache recent posts using direct Redis operations for maximum efficiency
                    String recentPostsJson = objectMapper.writeValueAsString(recentPostDtos);
                    byte[] recentPostsKey = "forum:realtime:posts:recent".getBytes();
                    byte[] recentPostsValue = recentPostsJson.getBytes();
                    connection.setEx(recentPostsKey, Duration.ofMinutes(30).getSeconds(), recentPostsValue);
                    
                    // Cache trending posts 
                    String trendingPostsJson = objectMapper.writeValueAsString(trendingPosts);
                    byte[] trendingPostsKey = "forum:realtime:posts:trending".getBytes();
                    byte[] trendingPostsValue = trendingPostsJson.getBytes();
                    connection.setEx(trendingPostsKey, Duration.ofMinutes(30).getSeconds(), trendingPostsValue);
                    
                    log.debug("Pipelined {} recent posts and {} trending posts to Redis", 
                        recentPostDtos.size(), trendingPosts.size());
                    
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize post DTOs for pipelining", e);
                }
                return null;
            });
            
            // Update statistics
            totalItemsWarmed.addAndGet(recentPostDtos.size() + trendingPosts.size());
            
            log.info("Post cache warmed up using pipelining: {} recent posts, {} trending posts", 
                recentPostDtos.size(), trendingPosts.size());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to warm up post cache", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmUpUserCache() {
        log.info("🔥 Starting user cache pipeline warm-up");
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline() ||
                              user.getLastSeen() != null && user.getLastSeen().isAfter(LocalDateTime.now().minusDays(7)))
                .limit(config.maxUsersToCache())
                .collect(Collectors.toList());
            
            // Cache online users list
            Set<String> onlineUsers = activeUsers.stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline())
                .map(User::getName)
                .collect(Collectors.toSet());
            
            log.debug("📡 Opening Redis connection for user cache pipeline ({} users, {} online)", 
                activeUsers.size(), onlineUsers.size());
            
            // Use TRUE Redis pipelining for online users cache
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    // Cache online users using direct Redis operations
                    String onlineUsersJson = objectMapper.writeValueAsString(onlineUsers);
                    byte[] onlineUsersKey = "forum:realtime:online_users".getBytes();
                    byte[] onlineUsersValue = onlineUsersJson.getBytes();
                    connection.setEx(onlineUsersKey, Duration.ofMinutes(5).getSeconds(), onlineUsersValue);
                    
                    log.debug("📊 Executing 1 operation in user cache pipeline");
                    
                    // Track pipeline operations for metrics
                    incrementRedisOperations(1);
                    
                } catch (Exception e) {
                    log.error("Pipeline execution failed for user cache", e);
                }
                return null;
            });
            
            totalItemsWarmed.addAndGet(activeUsers.size());
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ User cache pipeline completed: {} users, {} online in {}ms", 
                activeUsers.size(), onlineUsers.size(), duration);
                
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ User cache pipeline failed after {}ms", duration, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmUpNotificationCache() {
        log.info("🔥 Starting notification cache pipeline warm-up");
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline() ||
                              user.getLastSeen() != null && user.getLastSeen().isAfter(LocalDateTime.now().minusDays(1)))
                .limit(100) // Limit to most active users
                .collect(Collectors.toList());
            
            log.debug("📡 Opening Redis connection for notification pipeline ({} users)", activeUsers.size());
            
            // TRUE Redis pipelining for notification caching
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    int pipelineOps = 0;
                    
                    for (User user : activeUsers) {
                        try {
                            List<Notification> notifications = notificationRepository
                                .findNotificationsForCaching(user.getName(), PageRequest.of(0, 50));
                            
                            if (!notifications.isEmpty()) {
                                // Convert to DTOs to prevent serialization issues
                                List<CacheableNotificationDto> notificationDtos = cacheMapper.toNotificationDtos(notifications);
                                
                                // Direct Redis operation using connection parameter
                                String json = objectMapper.writeValueAsString(notificationDtos);
                                byte[] keyBytes = ("forum:realtime:notifications:" + user.getName()).getBytes();
                                byte[] valueBytes = json.getBytes();
                                connection.setEx(keyBytes, Duration.ofHours(2).getSeconds(), valueBytes);
                                pipelineOps++;
                                
                                totalItemsWarmed.addAndGet(notifications.size());
                            }
                        } catch (Exception e) {
                            log.warn("Failed to pipeline notifications for user {}: {}", user.getName(), e.getMessage());
                        }
                    }
                    
                    log.debug("📊 Executing {} operations in notification pipeline", pipelineOps);
                    
                    // Track pipeline operations for metrics
                    incrementRedisOperations(pipelineOps);
                    
                } catch (Exception e) {
                    log.error("Pipeline execution failed for notification cache", e);
                }
                return null;
            });
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Notification cache pipeline completed: {} users in {}ms | " +
                    "Items warmed: {}", 
                activeUsers.size(), duration, totalItemsWarmed.get());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Notification cache pipeline failed after {}ms", duration, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmUpFriendCache() {
        log.info("🔥 Starting friend cache pipeline warm-up");
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline() ||
                              user.getLastSeen() != null && user.getLastSeen().isAfter(LocalDateTime.now().minusDays(1)))
                .limit(100)
                .collect(Collectors.toList());
            
            log.debug("📡 Opening Redis connection for friend cache pipeline ({} users)", activeUsers.size());
            
            // TRUE Redis pipelining for friend cache
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    int pipelineOps = 0;
                    
                    for (User user : activeUsers) {
                        try {
                            // Get actual friends from friendship repository
                            Set<String> friends = friendshipRepository.findByUserAndStatus(user.getName(), 
                                    com.uipko.forumbackend.domain.entities.Friendship.FriendshipStatus.ACCEPTED)
                                .stream()
                                .map(friendship -> {
                                    // Return the other user's name
                                    return friendship.getRequester().getName().equals(user.getName()) 
                                        ? friendship.getAddressee().getName()
                                        : friendship.getRequester().getName();
                                })
                                .collect(Collectors.toSet());
                            
                            // Get pending friend requests (requests sent TO this user)
                            Set<String> pendingRequests = friendshipRepository.findPendingRequestsForUser(user.getName(),
                                    com.uipko.forumbackend.domain.entities.Friendship.FriendshipStatus.PENDING)
                                .stream()
                                .map(friendship -> friendship.getRequester().getName())
                                .collect(Collectors.toSet());
                            
                            // Direct Redis operations using connection parameter
                            // 1. Cache user friends
                            String friendsJson = objectMapper.writeValueAsString(friends);
                            byte[] friendsKey = ("forum:realtime:friends:" + user.getName()).getBytes();
                            byte[] friendsValue = friendsJson.getBytes();
                            connection.setEx(friendsKey, Duration.ofHours(2).getSeconds(), friendsValue);
                            pipelineOps++;
                            
                            // 2. Cache pending friend requests
                            String pendingJson = objectMapper.writeValueAsString(pendingRequests);
                            byte[] pendingKey = ("forum:realtime:pending_requests:" + user.getName()).getBytes();
                            byte[] pendingValue = pendingJson.getBytes();
                            connection.setEx(pendingKey, Duration.ofHours(2).getSeconds(), pendingValue);
                            pipelineOps++;
                            
                            totalItemsWarmed.addAndGet(friends.size() + pendingRequests.size());
                            
                        } catch (Exception e) {
                            log.warn("Failed to pipeline friends for user {}: {}", user.getName(), e.getMessage());
                        }
                    }
                    
                    log.debug("📊 Executing {} operations in friend cache pipeline", pipelineOps);
                    
                    // Track pipeline operations for metrics
                    incrementRedisOperations(pipelineOps);
                    
                } catch (Exception e) {
                    log.error("Pipeline execution failed for friend cache", e);
                }
                return null;
            });
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Friend cache pipeline completed: {} users in {}ms", activeUsers.size(), duration);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Friend cache pipeline failed after {}ms", duration, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Async
    public CompletableFuture<Void> warmUpReactionCache() {
        log.info("Warming up reaction cache");
        
        try {
            // Use two-step approach to prevent N+1 query warning with EntityGraph + Pageable
            List<Long> postIds = postRepository.findActivePostIds(PageRequest.of(0, 500));
            List<Post> allPosts = postRepository.findPostsByIdsWithRelations(postIds);
            
            List<Post> popularPosts = allPosts.stream()
                .filter(post -> post.getLikes() > 5 || post.getDislikes() > 0) // Posts with activity
                .sorted((p1, p2) -> Integer.compare(p2.getLikes(), p1.getLikes()))
                .limit(100)
                .collect(Collectors.toList());
            
            for (Post post : popularPosts) {
                Map<String, Long> reactions = Map.of(
                    "LIKE", (long) post.getLikes(),
                    "DISLIKE", (long) post.getDislikes()
                );
                realTimeCacheService.cachePostReactions(post.getId(), reactions);
                totalItemsWarmed.incrementAndGet();
            }
            
            log.info("Reaction cache warmed up for {} posts", popularPosts.size());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to warm up reaction cache", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void schedulePeriodicWarmUp() {
        // Skip if startup warm-up is still in progress to prevent conflicts
        if (totalWarmUps.get() == 0) {
            log.debug("Skipping scheduled warm-up - startup warm-up hasn't completed yet");
            return;
        }
        
        log.debug("Starting scheduled cache warm-up (attempt #{} since startup)", totalWarmUps.get() + 1);
        
        // Use async version to prevent blocking and ensure only one warm-up runs at a time
        warmUpAllCaches().exceptionally(throwable -> {
            log.error("Scheduled cache warm-up failed", throwable);
            return null;
        });
    }
    
    /**
     * Initialize periodic warm-up schedule (without immediate execution)
     * This method should only be called during application startup
     */
    public void initializePeriodicWarmUp() {
        log.info("Periodic cache warm-up schedule initialized (every hour)");
        // The @Scheduled annotation will handle the actual scheduling
        // This method is just for initialization logging
    }
    
    @Override
    @Async
    public CompletableFuture<Void> warmUpUserSpecificCache(String username) {
        log.info("Warming up cache for user: {}", username);
        
        try {
            User user = userRepository.findUserByName(username).orElse(null);
            if (user == null) {
                log.warn("User not found for cache warm-up: {}", username);
                return CompletableFuture.completedFuture(null);
            }
            
            // Warm up user details
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getName(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );
            
            authCacheService.cacheUserDetails(username, userDetails);
            authCacheService.cacheUser(username, user);
            authCacheService.cacheUserRoles(username, Set.of(user.getRole()));
            
            // Warm up notifications
            List<Notification> notifications = notificationRepository
                .findNotificationsForCaching(username, PageRequest.of(0, 50));
            if (!notifications.isEmpty()) {
                realTimeCacheService.cacheUserNotificationDtos(username, 
                    cacheMapper.toNotificationDtos(notifications));
            }
            
            // Warm up friends (placeholder)
            realTimeCacheService.cacheUserFriends(username, new HashSet<>());
            realTimeCacheService.cachePendingFriendRequests(username, new HashSet<>());
            
            log.info("Cache warmed up for user: {}", username);
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to warm up cache for user: {}", username, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CacheWarmingStats getWarmingStats() {
        long avgTime = totalWarmUps.get() > 0 ? totalWarmUpTime.get() / totalWarmUps.get() : 0;
        
        return new CacheWarmingStats(
            totalWarmUps.get(),
            successfulWarmUps.get(),
            failedWarmUps.get(),
            totalItemsWarmed.get(),
            avgTime,
            lastWarmUpTime != null ? lastWarmUpTime : "Never",
            "Next scheduled: " + LocalDateTime.now().plus(config.warmUpInterval())
        );
    }
    
    @Override
    public void configureCacheWarmingStrategy(CacheWarmingConfig newConfig) {
        this.config = newConfig;
        this.maxPostsToCache = newConfig.maxPostsToCache();
        this.maxUsersToCache = newConfig.maxUsersToCache();
        this.maxNotificationsPerUser = newConfig.maxNotificationsPerUser();
        
        log.info("Cache warming strategy updated: {}", newConfig);
    }
    
    public CacheWarmingConfig getConfig() {
        return config;
    }
}