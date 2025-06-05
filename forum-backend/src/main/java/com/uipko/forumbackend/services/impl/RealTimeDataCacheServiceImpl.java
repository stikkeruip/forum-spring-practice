package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.domain.dto.cache.CacheableNotificationDto;
import com.uipko.forumbackend.domain.dto.cache.CacheablePostDto;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.repositories.NotificationRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.RealTimeDataCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RealTimeDataCacheServiceImpl implements RealTimeDataCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    // Redis key patterns for real-time data
    private static final String ONLINE_USERS_KEY = "forum:realtime:online_users";
    private static final String USER_NOTIFICATIONS_KEY = "forum:realtime:notifications:";
    private static final String RECENT_POSTS_KEY = "forum:realtime:posts:recent";
    private static final String TRENDING_POSTS_KEY = "forum:realtime:posts:trending";
    private static final String USER_ACTIVITY_FEED_KEY = "forum:realtime:activity:user:";
    private static final String GLOBAL_ACTIVITY_FEED_KEY = "forum:realtime:activity:global";
    private static final String POST_REACTIONS_KEY = "forum:realtime:reactions:post:";
    private static final String COMMENT_REACTIONS_KEY = "forum:realtime:reactions:comment:";
    private static final String USER_FRIENDS_KEY = "forum:realtime:friends:";
    private static final String PENDING_REQUESTS_KEY = "forum:realtime:pending:";
    private static final String CACHE_STATS_KEY = "forum:realtime:stats";
    
    // Cache timeouts
    private static final Duration ONLINE_USERS_TTL = Duration.ofMinutes(5);
    private static final Duration NOTIFICATIONS_TTL = Duration.ofHours(2);
    private static final Duration POSTS_TTL = Duration.ofMinutes(30);
    private static final Duration ACTIVITY_TTL = Duration.ofHours(6);
    private static final Duration REACTIONS_TTL = Duration.ofMinutes(15);
    private static final Duration FRIENDS_TTL = Duration.ofHours(1);
    
    public RealTimeDataCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                      ObjectMapper objectMapper,
                                      PostRepository postRepository,
                                      NotificationRepository notificationRepository,
                                      UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.postRepository = postRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }
    
    // Online Users Management
    
    @Override
    public void cacheOnlineUsers(Set<String> onlineUsers) {
        try {
            String json = objectMapper.writeValueAsString(onlineUsers);
            redisTemplate.opsForValue().set(ONLINE_USERS_KEY, json, ONLINE_USERS_TTL);
            log.debug("Cached {} online users", onlineUsers.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache online users", e);
        }
    }
    
    @Override
    public void addOnlineUser(String username) {
        Set<String> onlineUsers = getCachedOnlineUsers();
        onlineUsers.add(username);
        cacheOnlineUsers(onlineUsers);
        log.debug("Added user {} to online cache", username);
    }
    
    @Override
    public void removeOnlineUser(String username) {
        Set<String> onlineUsers = getCachedOnlineUsers();
        onlineUsers.remove(username);
        cacheOnlineUsers(onlineUsers);
        log.debug("Removed user {} from online cache", username);
    }
    
    @Override
    public Set<String> getCachedOnlineUsers() {
        try {
            Object cached = redisTemplate.opsForValue().get(ONLINE_USERS_KEY);
            if (cached != null) {
                TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached online users", e);
        }
        return new HashSet<>();
    }
    
    @Override
    public boolean isUserOnlineCached(String username) {
        return getCachedOnlineUsers().contains(username);
    }
    
    @Override
    public long getOnlineUserCount() {
        return getCachedOnlineUsers().size();
    }
    
    // Live Notifications Management
    
    @Override
    @Transactional(readOnly = true)
    public void cacheUserNotifications(String username, List<Notification> notifications) {
        try {
            String json = objectMapper.writeValueAsString(notifications);
            redisTemplate.opsForValue().set(USER_NOTIFICATIONS_KEY + username, json, NOTIFICATIONS_TTL);
            log.debug("Cached {} notifications for user {}", notifications.size(), username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache notifications for user {}", username, e);
        }
    }
    
    @Override
    public void cacheUserNotificationDtos(String username, List<CacheableNotificationDto> notifications) {
        try {
            String json = objectMapper.writeValueAsString(notifications);
            redisTemplate.opsForValue().set(USER_NOTIFICATIONS_KEY + username, json, NOTIFICATIONS_TTL);
            log.debug("Cached {} notification DTOs for user {}", notifications.size(), username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache notification DTOs for user {}", username, e);
        }
    }
    
    @Override
    public void addNotificationToCache(String username, Notification notification) {
        List<Notification> notifications = getCachedUserNotifications(username);
        notifications.add(0, notification); // Add to beginning for most recent first
        
        // Keep only last 50 notifications in cache
        if (notifications.size() > 50) {
            notifications = notifications.subList(0, 50);
        }
        
        cacheUserNotifications(username, notifications);
        log.debug("Added notification {} to cache for user {}", notification.getId(), username);
    }
    
    @Override
    public List<Notification> getCachedUserNotifications(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_NOTIFICATIONS_KEY + username);
            if (cached != null) {
                TypeReference<List<Notification>> typeRef = new TypeReference<List<Notification>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached notifications for user {}", username, e);
        }
        return new ArrayList<>();
    }
    
    @Override
    public void markNotificationAsReadInCache(String username, Long notificationId) {
        List<Notification> notifications = getCachedUserNotifications(username);
        boolean updated = false;
        
        for (Notification notification : notifications) {
            if (notification.getId().equals(notificationId)) {
                notification.setRead(true);
                updated = true;
                break;
            }
        }
        
        if (updated) {
            cacheUserNotifications(username, notifications);
            log.debug("Marked notification {} as read in cache for user {}", notificationId, username);
        }
    }
    
    @Override
    public long getUnreadNotificationCount(String username) {
        List<Notification> notifications = getCachedUserNotifications(username);
        return notifications.stream()
            .filter(n -> !n.isRead())
            .count();
    }
    
    @Override
    public void clearUserNotificationsCache(String username) {
        redisTemplate.delete(USER_NOTIFICATIONS_KEY + username);
        log.debug("Cleared notifications cache for user {}", username);
    }
    
    // Live Posts Management
    
    @Override
    @Transactional(readOnly = true)
    public void cacheRecentPosts(List<Post> posts) {
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisTemplate.opsForValue().set(RECENT_POSTS_KEY, json, POSTS_TTL);
            log.debug("Cached {} recent posts", posts.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache recent posts", e);
        }
    }
    
    @Override
    public void cacheRecentPostDtos(List<CacheablePostDto> posts) {
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisTemplate.opsForValue().set(RECENT_POSTS_KEY, json, POSTS_TTL);
            log.debug("Cached {} recent post DTOs", posts.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache recent post DTOs", e);
        }
    }
    
    @Override
    public void addPostToRecentCache(Post post) {
        List<Post> posts = getCachedRecentPosts();
        posts.add(0, post); // Add to beginning
        
        // Keep only last 100 posts in cache
        if (posts.size() > 100) {
            posts = posts.subList(0, 100);
        }
        
        cacheRecentPosts(posts);
        log.debug("Added post {} to recent posts cache", post.getId());
    }
    
    @Override
    public void updatePostInCache(Post post) {
        List<Post> posts = getCachedRecentPosts();
        boolean updated = false;
        
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId().equals(post.getId())) {
                posts.set(i, post);
                updated = true;
                break;
            }
        }
        
        if (updated) {
            cacheRecentPosts(posts);
            log.debug("Updated post {} in recent posts cache", post.getId());
        }
    }
    
    @Override
    public void removePostFromCache(Long postId) {
        List<Post> posts = getCachedRecentPosts();
        boolean removed = posts.removeIf(post -> post.getId().equals(postId));
        
        if (removed) {
            cacheRecentPosts(posts);
            log.debug("Removed post {} from recent posts cache", postId);
        }
    }
    
    @Override
    public List<Post> getCachedRecentPosts() {
        try {
            Object cached = redisTemplate.opsForValue().get(RECENT_POSTS_KEY);
            if (cached != null) {
                TypeReference<List<Post>> typeRef = new TypeReference<List<Post>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached recent posts", e);
        }
        return new ArrayList<>();
    }
    
    @Override
    @Transactional(readOnly = true)
    public void cacheTrendingPosts(List<Post> posts) {
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisTemplate.opsForValue().set(TRENDING_POSTS_KEY, json, POSTS_TTL);
            log.debug("Cached {} trending posts", posts.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache trending posts", e);
        }
    }
    
    @Override
    public void cacheTrendingPostDtos(List<CacheablePostDto> posts) {
        try {
            String json = objectMapper.writeValueAsString(posts);
            redisTemplate.opsForValue().set(TRENDING_POSTS_KEY, json, POSTS_TTL);
            log.debug("Cached {} trending post DTOs", posts.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache trending post DTOs", e);
        }
    }
    
    @Override
    public List<Post> getCachedTrendingPosts() {
        try {
            Object cached = redisTemplate.opsForValue().get(TRENDING_POSTS_KEY);
            if (cached != null) {
                TypeReference<List<Post>> typeRef = new TypeReference<List<Post>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached trending posts", e);
        }
        return new ArrayList<>();
    }
    
    // Live Activity Feed
    
    @Override
    public void cacheUserActivityFeed(String username, List<ActivityEvent> activities) {
        try {
            String json = objectMapper.writeValueAsString(activities);
            redisTemplate.opsForValue().set(USER_ACTIVITY_FEED_KEY + username, json, ACTIVITY_TTL);
            log.debug("Cached {} activities for user {}", activities.size(), username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache activity feed for user {}", username, e);
        }
    }
    
    @Override
    public void addActivityToFeed(String username, ActivityEvent activity) {
        List<ActivityEvent> activities = getCachedUserActivityFeed(username);
        activities.add(0, activity); // Add to beginning
        
        // Keep only last 200 activities in cache
        if (activities.size() > 200) {
            activities = activities.subList(0, 200);
        }
        
        cacheUserActivityFeed(username, activities);
        log.debug("Added activity {} to feed for user {}", activity.eventId(), username);
    }
    
    @Override
    public List<ActivityEvent> getCachedUserActivityFeed(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_ACTIVITY_FEED_KEY + username);
            if (cached != null) {
                TypeReference<List<ActivityEvent>> typeRef = new TypeReference<List<ActivityEvent>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached activity feed for user {}", username, e);
        }
        return new ArrayList<>();
    }
    
    @Override
    public void cacheGlobalActivityFeed(List<ActivityEvent> activities) {
        try {
            String json = objectMapper.writeValueAsString(activities);
            redisTemplate.opsForValue().set(GLOBAL_ACTIVITY_FEED_KEY, json, ACTIVITY_TTL);
            log.debug("Cached {} global activities", activities.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to cache global activity feed", e);
        }
    }
    
    @Override
    public List<ActivityEvent> getCachedGlobalActivityFeed() {
        try {
            Object cached = redisTemplate.opsForValue().get(GLOBAL_ACTIVITY_FEED_KEY);
            if (cached != null) {
                TypeReference<List<ActivityEvent>> typeRef = new TypeReference<List<ActivityEvent>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached global activity feed", e);
        }
        return new ArrayList<>();
    }
    
    // Live Reactions Cache
    
    @Override
    public void cachePostReactions(Long postId, Map<String, Long> reactionCounts) {
        try {
            String json = objectMapper.writeValueAsString(reactionCounts);
            redisTemplate.opsForValue().set(POST_REACTIONS_KEY + postId, json, REACTIONS_TTL);
            log.debug("Cached reactions for post {}", postId);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache reactions for post {}", postId, e);
        }
    }
    
    @Override
    public void updatePostReactionInCache(Long postId, String reactionType, Long newCount) {
        Map<String, Long> reactions = getCachedPostReactions(postId);
        reactions.put(reactionType, newCount);
        cachePostReactions(postId, reactions);
        log.debug("Updated {} reaction count to {} for post {}", reactionType, newCount, postId);
    }
    
    @Override
    public Map<String, Long> getCachedPostReactions(Long postId) {
        try {
            Object cached = redisTemplate.opsForValue().get(POST_REACTIONS_KEY + postId);
            if (cached != null) {
                TypeReference<Map<String, Long>> typeRef = new TypeReference<Map<String, Long>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached reactions for post {}", postId, e);
        }
        return new HashMap<>();
    }
    
    @Override
    public void cacheCommentReactions(Long commentId, Map<String, Long> reactionCounts) {
        try {
            String json = objectMapper.writeValueAsString(reactionCounts);
            redisTemplate.opsForValue().set(COMMENT_REACTIONS_KEY + commentId, json, REACTIONS_TTL);
            log.debug("Cached reactions for comment {}", commentId);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache reactions for comment {}", commentId, e);
        }
    }
    
    @Override
    public Map<String, Long> getCachedCommentReactions(Long commentId) {
        try {
            Object cached = redisTemplate.opsForValue().get(COMMENT_REACTIONS_KEY + commentId);
            if (cached != null) {
                TypeReference<Map<String, Long>> typeRef = new TypeReference<Map<String, Long>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached reactions for comment {}", commentId, e);
        }
        return new HashMap<>();
    }
    
    // Live Friend Status Cache
    
    @Override
    public void cacheUserFriends(String username, Set<String> friends) {
        try {
            String json = objectMapper.writeValueAsString(friends);
            redisTemplate.opsForValue().set(USER_FRIENDS_KEY + username, json, FRIENDS_TTL);
            log.debug("Cached {} friends for user {}", friends.size(), username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache friends for user {}", username, e);
        }
    }
    
    @Override
    public void addFriendToCache(String username, String friendUsername) {
        Set<String> friends = getCachedUserFriends(username);
        friends.add(friendUsername);
        cacheUserFriends(username, friends);
        log.debug("Added friend {} to cache for user {}", friendUsername, username);
    }
    
    @Override
    public void removeFriendFromCache(String username, String friendUsername) {
        Set<String> friends = getCachedUserFriends(username);
        friends.remove(friendUsername);
        cacheUserFriends(username, friends);
        log.debug("Removed friend {} from cache for user {}", friendUsername, username);
    }
    
    @Override
    public Set<String> getCachedUserFriends(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_FRIENDS_KEY + username);
            if (cached != null) {
                TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached friends for user {}", username, e);
        }
        return new HashSet<>();
    }
    
    @Override
    public void cachePendingFriendRequests(String username, Set<String> pendingRequests) {
        try {
            String json = objectMapper.writeValueAsString(pendingRequests);
            redisTemplate.opsForValue().set(PENDING_REQUESTS_KEY + username, json, FRIENDS_TTL);
            log.debug("Cached {} pending requests for user {}", pendingRequests.size(), username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache pending requests for user {}", username, e);
        }
    }
    
    @Override
    public Set<String> getCachedPendingFriendRequests(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(PENDING_REQUESTS_KEY + username);
            if (cached != null) {
                TypeReference<Set<String>> typeRef = new TypeReference<Set<String>>() {};
                return objectMapper.readValue(cached.toString(), typeRef);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cached pending requests for user {}", username, e);
        }
        return new HashSet<>();
    }
    
    // Cache Warming and Management
    
    @Override
    @Transactional(readOnly = true)
    public void warmUpRealTimeCaches() {
        log.info("🔥 Starting real-time cache warm-up with pipelining optimization");
        long startTime = System.currentTimeMillis();
        
        try {
            // Use two-step approach to prevent N+1 query warning with EntityGraph + Pageable
            List<Long> postIds = postRepository.findActivePostIds(PageRequest.of(0, 100));
            List<Post> recentPosts = postRepository.findPostsByIdsWithRelations(postIds);
            
            // Get active users for notification caching
            List<String> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getIsOnline() != null && user.getIsOnline())
                .map(user -> user.getName())
                .limit(100) // Limit for performance
                .collect(Collectors.toList());
            
            // Use single Redis pipeline for ALL operations to minimize connection overhead
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                try {
                    // 1. Pipeline post caching operations
                    List<CacheablePostDto> recentPostDtos = recentPosts.stream()
                        .map(post -> {
                            try {
                                return CacheablePostDto.builder()
                                    .id(post.getId())
                                    .userName(post.getUser() != null ? post.getUser().getName() : null)
                                    .userRole(post.getUser() != null ? post.getUser().getRole() : null)
                                    .createdDate(post.getCreatedDate())
                                    .updatedDate(post.getUpdatedDate())
                                    .deletedDate(post.getDeletedDate())
                                    .deletedByUserName(post.getDeletedBy() != null ? post.getDeletedBy().getName() : null)
                                    .title(post.getTitle())
                                    .content(post.getContent())
                                    .likes(post.getLikes())
                                    .dislikes(post.getDislikes())
                                    .commentCount(post.getComments() != null && 
                                        org.hibernate.Hibernate.isInitialized(post.getComments()) ? 
                                        post.getComments().size() : 0)
                                    .build();
                            } catch (Exception e) {
                                log.warn("Failed to convert post {} to DTO: {}", post.getId(), e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    
                    // Pipeline recent posts cache
                    if (!recentPostDtos.isEmpty()) {
                        String recentPostsJson = objectMapper.writeValueAsString(recentPostDtos);
                        byte[] recentPostsKey = RECENT_POSTS_KEY.getBytes();
                        byte[] recentPostsValue = recentPostsJson.getBytes();
                        connection.setEx(recentPostsKey, POSTS_TTL.getSeconds(), recentPostsValue);
                    }
                    
                    // Pipeline trending posts (subset of recent)
                    List<CacheablePostDto> trendingPosts = recentPostDtos.stream()
                        .filter(post -> post.getCreatedDate().isAfter(LocalDateTime.now().minusHours(24)))
                        .sorted((p1, p2) -> Integer.compare(p2.getLikes(), p1.getLikes()))
                        .limit(20)
                        .collect(Collectors.toList());
                    
                    if (!trendingPosts.isEmpty()) {
                        String trendingPostsJson = objectMapper.writeValueAsString(trendingPosts);
                        byte[] trendingPostsKey = TRENDING_POSTS_KEY.getBytes();
                        byte[] trendingPostsValue = trendingPostsJson.getBytes();
                        connection.setEx(trendingPostsKey, POSTS_TTL.getSeconds(), trendingPostsValue);
                    }
                    
                    // 2. Pipeline notification caching for active users
                    for (String username : activeUsers) {
                        try {
                            List<Notification> notifications = notificationRepository
                                .findNotificationsForCaching(username, PageRequest.of(0, 50));
                            
                            if (!notifications.isEmpty()) {
                                List<CacheableNotificationDto> notificationDtos = notifications.stream()
                                    .map(n -> CacheableNotificationDto.builder()
                                        .id(n.getId())
                                        .recipientName(n.getRecipient() != null ? n.getRecipient().getName() : null)
                                        .actorName(n.getActor() != null ? n.getActor().getName() : null)
                                        .type(n.getType() != null ? n.getType().name() : null)
                                        .message(n.getMessage())
                                        .read(n.isRead())
                                        .createdDate(n.getCreatedDate())
                                        .targetPostId(n.getTargetPost() != null ? n.getTargetPost().getId() : null)
                                        .targetPostTitle(n.getTargetPost() != null ? n.getTargetPost().getTitle() : null)
                                        .targetCommentId(n.getTargetComment() != null ? n.getTargetComment().getId() : null)
                                        .build())
                                    .collect(Collectors.toList());
                                
                                String json = objectMapper.writeValueAsString(notificationDtos);
                                byte[] keyBytes = (USER_NOTIFICATIONS_KEY + username).getBytes();
                                byte[] valueBytes = json.getBytes();
                                connection.setEx(keyBytes, NOTIFICATIONS_TTL.getSeconds(), valueBytes);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to cache notifications for user {}: {}", username, e.getMessage());
                        }
                    }
                    
                    // 3. Pipeline online users cache
                    Set<String> onlineUsers = activeUsers.stream()
                        .collect(Collectors.toSet());
                    String onlineUsersJson = objectMapper.writeValueAsString(onlineUsers);
                    byte[] onlineUsersKey = ONLINE_USERS_KEY.getBytes();
                    byte[] onlineUsersValue = onlineUsersJson.getBytes();
                    connection.setEx(onlineUsersKey, ONLINE_USERS_TTL.getSeconds(), onlineUsersValue);
                    
                } catch (Exception e) {
                    log.error("Failed during pipelined cache operations", e);
                }
                return null;
            });
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Real-time cache warm-up completed in {}ms using single Redis pipeline | " +
                    "Posts: {}, Users: {}, Notifications batched: {}", 
                duration, recentPosts.size(), activeUsers.size(), activeUsers.size());
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Real-time cache warm-up failed after {}ms", duration, e);
        }
    }
    
    @Override
    public void refreshCache(String cacheType) {
        switch (cacheType.toLowerCase()) {
            case "posts":
                List<Long> postIds = postRepository.findActivePostIds(PageRequest.of(0, 100));
                List<Post> recentPosts = postRepository.findPostsByIdsWithRelations(postIds);
                // Convert to DTOs for safe caching
                cacheRecentPostDtos(recentPosts.stream()
                    .map(post -> {
                        try {
                            return CacheablePostDto.builder()
                                .id(post.getId())
                                .userName(post.getUser() != null ? post.getUser().getName() : null)
                                .userRole(post.getUser() != null ? post.getUser().getRole() : null)
                                .createdDate(post.getCreatedDate())
                                .updatedDate(post.getUpdatedDate())
                                .deletedDate(post.getDeletedDate())
                                .deletedByUserName(post.getDeletedBy() != null ? post.getDeletedBy().getName() : null)
                                .title(post.getTitle())
                                .content(post.getContent())
                                .likes(post.getLikes())
                                .dislikes(post.getDislikes())
                                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                                .build();
                        } catch (Exception e) {
                            log.warn("Failed to convert post {} to DTO during refresh", post.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
                break;
            case "online":
                // This should be refreshed through the RedisMessagingService
                log.debug("Online users cache refresh requested");
                break;
            default:
                log.warn("Unknown cache type for refresh: {}", cacheType);
        }
    }
    
    @Override
    public void clearAllRealTimeCaches() {
        Set<String> keys = redisTemplate.keys("forum:realtime:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cleared {} real-time cache keys", keys.size());
        }
    }
    
    @Override
    public RealTimeCacheStats getCacheStats() {
        try {
            long onlineUsers = getOnlineUserCount();
            long recentPostsCount = getCachedRecentPosts().size();
            long trendingPostsCount = getCachedTrendingPosts().size();
            
            // Get cache key counts
            Set<String> notificationKeys = redisTemplate.keys(USER_NOTIFICATIONS_KEY + "*");
            Set<String> reactionKeys = redisTemplate.keys(POST_REACTIONS_KEY + "*");
            Set<String> activityKeys = redisTemplate.keys(USER_ACTIVITY_FEED_KEY + "*");
            
            long totalNotifications = notificationKeys != null ? notificationKeys.size() : 0;
            long totalReactions = reactionKeys != null ? reactionKeys.size() : 0;
            long totalActivities = activityKeys != null ? activityKeys.size() : 0;
            
            return new RealTimeCacheStats(
                onlineUsers,
                totalNotifications,
                recentPostsCount,
                trendingPostsCount,
                totalActivities,
                totalReactions,
                0.0, // Average response time would need additional tracking
                85L  // Cache hit rate would need additional tracking
            );
        } catch (Exception e) {
            log.error("Failed to retrieve real-time cache stats", e);
            return new RealTimeCacheStats(0, 0, 0, 0, 0, 0, 0.0, 0L);
        }
    }
    
    @Override
    public void setCacheExpiration(String cacheKey, Duration expiration) {
        redisTemplate.expire(cacheKey, expiration);
        log.debug("Set expiration {} for cache key {}", expiration, cacheKey);
    }
}