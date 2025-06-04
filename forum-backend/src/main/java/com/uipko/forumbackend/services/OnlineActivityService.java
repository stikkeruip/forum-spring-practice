package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.dto.OnlineUserDto;

import java.util.List;

public interface OnlineActivityService {
    
    /**
     * Mark user as online when they connect via WebSocket
     */
    void setUserOnline(String username);
    
    /**
     * Mark user as offline when they disconnect from WebSocket
     */
    void setUserOffline(String username);
    
    /**
     * Update user's last seen timestamp (heartbeat)
     */
    void updateLastSeen(String username);
    
    /**
     * Get list of currently online users
     */
    List<OnlineUserDto> getOnlineUsers();
    
    /**
     * Get count of online users
     */
    long getOnlineUserCount();
    
    /**
     * Check if a specific user is online
     */
    boolean isUserOnline(String username);
    
    /**
     * Clean up stale online users (users who haven't sent heartbeat recently)
     * This should be called periodically
     */
    void cleanupStaleUsers();
}