package com.uipko.forumbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
@Slf4j
public class RedisMessageListeners {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisMessageListeners(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Notification message listener
     */
    @Bean
    public MessageListenerAdapter notificationListener() {
        return new MessageListenerAdapter(new NotificationRedisListener(messagingTemplate, objectMapper), "receiveMessage");
    }
    
    /**
     * Post updates message listener
     */
    @Bean
    public MessageListenerAdapter postUpdatesListener() {
        return new MessageListenerAdapter(new PostUpdatesRedisListener(messagingTemplate, objectMapper), "receiveMessage");
    }
    
    /**
     * User activity message listener
     */
    @Bean
    public MessageListenerAdapter userActivityListener() {
        return new MessageListenerAdapter(new UserActivityRedisListener(messagingTemplate, objectMapper), "receiveMessage");
    }
    
    /**
     * Friend updates message listener
     */
    @Bean
    public MessageListenerAdapter friendUpdatesListener() {
        return new MessageListenerAdapter(new FriendUpdatesRedisListener(messagingTemplate, objectMapper), "receiveMessage");
    }
    
    /**
     * Configure message listeners for existing container
     */
    @Bean
    public RedisMessageListenerContainerConfigurer redisListenerConfigurer(
            RedisMessageListenerContainer container,
            MessageListenerAdapter notificationListener,
            MessageListenerAdapter postUpdatesListener,
            MessageListenerAdapter userActivityListener,
            MessageListenerAdapter friendUpdatesListener) {
        
        // Add listeners to their respective channels
        container.addMessageListener(notificationListener, new ChannelTopic(CacheConfig.NOTIFICATION_CHANNEL));
        container.addMessageListener(postUpdatesListener, new ChannelTopic(CacheConfig.POST_UPDATES_CHANNEL));
        container.addMessageListener(userActivityListener, new ChannelTopic(CacheConfig.USER_ACTIVITY_CHANNEL));
        container.addMessageListener(friendUpdatesListener, new ChannelTopic(CacheConfig.FRIEND_UPDATES_CHANNEL));
        
        log.info("Redis message listener container configured with {} listeners", 4);
        return new RedisMessageListenerContainerConfigurer();
    }
    
    /**
     * Simple configuration marker class
     */
    public static class RedisMessageListenerContainerConfigurer {
        // Empty marker class to return from bean method
    }
    
    /**
     * Notification Redis listener for real-time notifications
     */
    public static class NotificationRedisListener {
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper;
        
        public NotificationRedisListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
            this.messagingTemplate = messagingTemplate;
            this.objectMapper = objectMapper;
        }
        
        public void receiveMessage(Object message) {
            try {
                log.debug("Received notification message from Redis: {}", message);
                
                // The message is already deserialized as a Map by Redis
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> notificationData = (java.util.Map<String, Object>) message;
                String username = (String) notificationData.get("username");
                
                // Forward to specific user via WebSocket
                messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notificationData);
                
                // Also broadcast to general notification topic
                messagingTemplate.convertAndSend("/topic/notifications", notificationData);
                
                log.debug("Forwarded notification to WebSocket for user: {}", username);
            } catch (Exception e) {
                log.error("Error processing notification message from Redis", e);
            }
        }
    }
    
    /**
     * Post updates Redis listener for real-time post events
     */
    public static class PostUpdatesRedisListener {
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper;
        
        public PostUpdatesRedisListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
            this.messagingTemplate = messagingTemplate;
            this.objectMapper = objectMapper;
        }
        
        public void receiveMessage(Object message) {
            try {
                log.debug("Received post update message from Redis: {}", message);
                
                // The message is already deserialized by Redis
                // It could be a PostEvent object or a Map depending on the publisher
                Object postEventData = message;
                
                // Broadcast to all connected clients
                messagingTemplate.convertAndSend("/topic/post-updates", postEventData);
                
                // Extract action for logging if it's a Map
                if (postEventData instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> eventMap = (java.util.Map<String, Object>) postEventData;
                    log.debug("Broadcasted post update: {}", eventMap.get("action"));
                } else {
                    log.debug("Broadcasted post update event");
                }
            } catch (Exception e) {
                log.error("Error processing post update message from Redis", e);
            }
        }
    }
    
    /**
     * User activity Redis listener for online status updates
     */
    public static class UserActivityRedisListener {
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper;
        
        public UserActivityRedisListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
            this.messagingTemplate = messagingTemplate;
            this.objectMapper = objectMapper;
        }
        
        public void receiveMessage(Object message) {
            try {
                log.debug("Received user activity message from Redis: {}", message);
                
                // The message is already deserialized as a Map by Redis
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> activityData = (java.util.Map<String, Object>) message;
                String username = (String) activityData.get("username");
                String action = (String) activityData.get("action");
                
                // Broadcast to appropriate topics
                if ("ONLINE".equals(action) || "OFFLINE".equals(action)) {
                    messagingTemplate.convertAndSend("/topic/user-status", activityData);
                    messagingTemplate.convertAndSend("/topic/online-users", activityData);
                } else {
                    messagingTemplate.convertAndSend("/topic/user-activity", activityData);
                }
                
                log.debug("Broadcasted user activity: {} - {}", username, action);
            } catch (Exception e) {
                log.error("Error processing user activity message from Redis", e);
            }
        }
    }
    
    /**
     * Friend updates Redis listener for friend-related events
     */
    public static class FriendUpdatesRedisListener {
        private final SimpMessagingTemplate messagingTemplate;
        private final ObjectMapper objectMapper;
        
        public FriendUpdatesRedisListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
            this.messagingTemplate = messagingTemplate;
            this.objectMapper = objectMapper;
        }
        
        public void receiveMessage(String message) {
            try {
                log.debug("Received friend update message from Redis: {}", message);
                
                // Parse the friend data
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> friendData = objectMapper.readValue(message, java.util.Map.class);
                String username = (String) friendData.get("username");
                
                // Send to specific user
                messagingTemplate.convertAndSendToUser(username, "/queue/friend-updates", friendData);
                
                // Also broadcast to general topic
                messagingTemplate.convertAndSend("/topic/friend-updates", friendData);
                
                log.debug("Broadcasted friend update for user: {}", username);
            } catch (Exception e) {
                log.error("Error processing friend update message from Redis", e);
            }
        }
    }
}