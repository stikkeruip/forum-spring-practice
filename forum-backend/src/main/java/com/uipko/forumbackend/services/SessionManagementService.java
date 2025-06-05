package com.uipko.forumbackend.services;

import java.time.Duration;
import java.util.Set;

/**
 * Service for managing user sessions across multiple application instances
 * Integrates JWT tokens with Redis-based session storage
 */
public interface SessionManagementService {
    
    /**
     * Store user session data in Redis
     */
    void createUserSession(String username, String sessionId, String jwtToken);
    
    /**
     * Validate if a JWT token is associated with an active session
     */
    boolean isValidSession(String username, String jwtToken);
    
    /**
     * Invalidate a specific user session
     */
    void invalidateSession(String username, String sessionId);
    
    /**
     * Invalidate all sessions for a user (logout from all devices)
     */
    void invalidateAllUserSessions(String username);
    
    /**
     * Get all active session IDs for a user
     */
    Set<String> getActiveSessionIds(String username);
    
    /**
     * Extend session expiration
     */
    void refreshSession(String username, String sessionId);
    
    /**
     * Store additional session attributes
     */
    void setSessionAttribute(String username, String sessionId, String key, Object value);
    
    /**
     * Get session attribute
     */
    Object getSessionAttribute(String username, String sessionId, String key);
    
    /**
     * Check if user has active sessions
     */
    boolean hasActiveSessions(String username);
    
    /**
     * Get session info for monitoring
     */
    SessionInfo getSessionInfo(String username, String sessionId);
    
    /**
     * Clean up expired sessions
     */
    void cleanupExpiredSessions();
    
    /**
     * Session information DTO
     */
    record SessionInfo(
        String sessionId,
        String username,
        String createdAt,
        String lastAccessed,
        String userAgent,
        String ipAddress,
        boolean active
    ) {}
}