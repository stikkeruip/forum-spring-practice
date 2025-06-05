package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.config.CacheConfig;
import com.uipko.forumbackend.domain.events.PostEvent;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.services.RedisMessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class RedisMessagingServiceImpl implements RedisMessagingService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis keys for online user tracking
    private static final String ONLINE_USERS_KEY = "forum:online_users";
    private static final String USER_LAST_SEEN_KEY = "forum:user_last_seen:";
    
    public RedisMessagingServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void publishNotification(Notification notification) {
        try {
            Map<String, Object> notificationData = Map.of(
                "id", notification.getId(),
                "username", notification.getRecipient().getName(),
                "type", notification.getType(),
                "message", notification.getMessage(),
                "isRead", notification.isRead(),
                "createdAt", notification.getCreatedDate(),
                "timestamp", LocalDateTime.now()
            );
            
            redisTemplate.convertAndSend(CacheConfig.NOTIFICATION_CHANNEL, notificationData);
            
            log.debug("Published notification to channel {}: {}", CacheConfig.NOTIFICATION_CHANNEL, notification.getId());
        } catch (Exception e) {
            log.error("Failed to publish notification", e);
        }
    }
    
    @Override
    public void publishPostEvent(PostEvent postEvent) {
        try {
            redisTemplate.convertAndSend(CacheConfig.POST_UPDATES_CHANNEL, postEvent);
            
            log.debug("Published post event to channel {}: {}", CacheConfig.POST_UPDATES_CHANNEL, postEvent.getAction());
        } catch (Exception e) {
            log.error("Failed to publish post event", e);
        }
    }
    
    @Override
    public void publishUserActivity(String username, String action, Object data) {
        try {
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("username", username);
            activityData.put("action", action); // ONLINE, OFFLINE, TYPING, IDLE
            activityData.put("timestamp", LocalDateTime.now());
            if (data != null) {
                activityData.put("data", data);
            }
            
            redisTemplate.convertAndSend(CacheConfig.USER_ACTIVITY_CHANNEL, activityData);
            
            log.debug("Published user activity: {} - {}", username, action);
        } catch (Exception e) {
            log.error("Failed to publish user activity", e);
        }
    }
    
    @Override
    public void publishFriendUpdate(String username, String action, Object data) {
        try {
            Map<String, Object> friendData = new HashMap<>();
            friendData.put("username", username);
            friendData.put("action", action); // FRIEND_REQUEST, FRIEND_ACCEPTED, FRIEND_REMOVED
            friendData.put("timestamp", LocalDateTime.now());
            if (data != null) {
                friendData.put("data", data);
            }
            
            redisTemplate.convertAndSend(CacheConfig.FRIEND_UPDATES_CHANNEL, friendData);
            
            log.debug("Published friend update: {} - {}", username, action);
        } catch (Exception e) {
            log.error("Failed to publish friend update", e);
        }
    }
    
    @Override
    public void setUserOnline(String username) {
        // Add to online users set
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, username);
        
        // Set last seen timestamp
        redisTemplate.opsForValue().set(
            USER_LAST_SEEN_KEY + username, 
            LocalDateTime.now().toString(),
            Duration.ofHours(24)
        );
        
        // Publish user activity
        publishUserActivity(username, "ONLINE", null);
        
        log.debug("User {} is now online", username);
    }
    
    @Override
    public void setUserOffline(String username) {
        // Remove from online users set
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, username);
        
        // Update last seen timestamp
        redisTemplate.opsForValue().set(
            USER_LAST_SEEN_KEY + username, 
            LocalDateTime.now().toString(),
            Duration.ofDays(30) // Keep last seen for 30 days
        );
        
        // Publish user activity
        publishUserActivity(username, "OFFLINE", null);
        
        log.debug("User {} is now offline", username);
    }
    
    @Override
    public Set<String> getOnlineUsers() {
        Set<Object> onlineUsers = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        return onlineUsers != null ? 
            onlineUsers.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
            java.util.Collections.emptySet();
    }
    
    @Override
    public boolean isUserOnline(String username) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, username));
    }
    
    /**
     * Get user's last seen timestamp
     */
    public String getUserLastSeen(String username) {
        Object lastSeen = redisTemplate.opsForValue().get(USER_LAST_SEEN_KEY + username);
        return lastSeen != null ? lastSeen.toString() : null;
    }
    
    /**
     * Update user heartbeat (for keeping them online)
     */
    public void updateUserHeartbeat(String username) {
        if (isUserOnline(username)) {
            redisTemplate.opsForValue().set(
                USER_LAST_SEEN_KEY + username,
                LocalDateTime.now().toString(),
                Duration.ofHours(24)
            );
        }
    }
    
    /**
     * Clean up offline users (called periodically)
     */
    public void cleanupOfflineUsers() {
        Set<String> onlineUsers = getOnlineUsers();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5 minutes timeout
        
        for (String username : onlineUsers) {
            String lastSeenStr = getUserLastSeen(username);
            if (lastSeenStr != null) {
                try {
                    LocalDateTime lastSeen = LocalDateTime.parse(lastSeenStr);
                    if (lastSeen.isBefore(threshold)) {
                        setUserOffline(username);
                        log.debug("Auto-removed offline user: {}", username);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse last seen for user {}: {}", username, lastSeenStr);
                }
            }
        }
    }
}