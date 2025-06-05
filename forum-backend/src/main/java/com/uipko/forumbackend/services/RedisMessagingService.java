package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.events.PostEvent;
import com.uipko.forumbackend.domain.entities.Notification;

public interface RedisMessagingService {
    
    /**
     * Publish notification to all connected clients
     */
    void publishNotification(Notification notification);
    
    /**
     * Publish post event for real-time updates
     */
    void publishPostEvent(PostEvent postEvent);
    
    /**
     * Publish user activity (online/offline status)
     */
    void publishUserActivity(String username, String action, Object data);
    
    /**
     * Publish friend-related updates
     */
    void publishFriendUpdate(String username, String action, Object data);
    
    /**
     * Track user online status
     */
    void setUserOnline(String username);
    
    /**
     * Track user offline status
     */
    void setUserOffline(String username);
    
    /**
     * Get currently online users
     */
    java.util.Set<String> getOnlineUsers();
    
    /**
     * Check if user is online
     */
    boolean isUserOnline(String username);
}