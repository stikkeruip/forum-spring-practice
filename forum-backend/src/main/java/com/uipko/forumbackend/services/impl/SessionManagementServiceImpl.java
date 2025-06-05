package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.services.SessionManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SessionManagementServiceImpl implements SessionManagementService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis key patterns
    private static final String USER_SESSIONS_KEY = "forum:sessions:user:";
    private static final String SESSION_DATA_KEY = "forum:sessions:data:";
    private static final String SESSION_TOKEN_KEY = "forum:sessions:token:";
    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(30);
    
    public SessionManagementServiceImpl(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void createUserSession(String username, String sessionId, String jwtToken) {
        try {
            // Add session ID to user's active sessions set
            redisTemplate.opsForSet().add(USER_SESSIONS_KEY + username, sessionId);
            redisTemplate.expire(USER_SESSIONS_KEY + username, SESSION_TIMEOUT);
            
            // Store session data
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("username", username);
            sessionData.put("sessionId", sessionId);
            sessionData.put("jwtToken", jwtToken);
            sessionData.put("createdAt", LocalDateTime.now().toString());
            sessionData.put("lastAccessed", LocalDateTime.now().toString());
            sessionData.put("active", true);
            
            String sessionDataJson = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(SESSION_DATA_KEY + sessionId, sessionDataJson, SESSION_TIMEOUT);
            
            // Store token-to-session mapping for quick lookup
            redisTemplate.opsForValue().set(SESSION_TOKEN_KEY + jwtToken, sessionId, SESSION_TIMEOUT);
            
            log.debug("Created session {} for user {}", sessionId, username);
        } catch (JsonProcessingException e) {
            log.error("Failed to create session for user {}", username, e);
        }
    }
    
    @Override
    public boolean isValidSession(String username, String jwtToken) {
        try {
            // Get session ID from token
            Object sessionIdObj = redisTemplate.opsForValue().get(SESSION_TOKEN_KEY + jwtToken);
            if (sessionIdObj == null) {
                return false;
            }
            
            String sessionId = sessionIdObj.toString();
            
            // Check if session exists in user's active sessions
            Boolean isMember = redisTemplate.opsForSet().isMember(USER_SESSIONS_KEY + username, sessionId);
            if (!Boolean.TRUE.equals(isMember)) {
                return false;
            }
            
            // Check if session data exists and is active
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj == null) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
            Boolean active = (Boolean) sessionData.get("active");
            
            if (Boolean.TRUE.equals(active)) {
                // Update last accessed time
                refreshSession(username, sessionId);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error validating session for user {} with token {}", username, jwtToken, e);
            return false;
        }
    }
    
    @Override
    public void invalidateSession(String username, String sessionId) {
        try {
            // Remove from user's active sessions
            redisTemplate.opsForSet().remove(USER_SESSIONS_KEY + username, sessionId);
            
            // Get session data to extract JWT token
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
                String jwtToken = (String) sessionData.get("jwtToken");
                
                // Remove token mapping
                if (jwtToken != null) {
                    redisTemplate.delete(SESSION_TOKEN_KEY + jwtToken);
                }
            }
            
            // Remove session data
            redisTemplate.delete(SESSION_DATA_KEY + sessionId);
            
            log.debug("Invalidated session {} for user {}", sessionId, username);
        } catch (Exception e) {
            log.error("Error invalidating session {} for user {}", sessionId, username, e);
        }
    }
    
    @Override
    public void invalidateAllUserSessions(String username) {
        Set<String> sessionIds = getActiveSessionIds(username);
        
        for (String sessionId : sessionIds) {
            invalidateSession(username, sessionId);
        }
        
        // Clear the user's session set
        redisTemplate.delete(USER_SESSIONS_KEY + username);
        
        log.debug("Invalidated all sessions for user {}", username);
    }
    
    @Override
    public Set<String> getActiveSessionIds(String username) {
        Set<Object> sessionObjects = redisTemplate.opsForSet().members(USER_SESSIONS_KEY + username);
        return sessionObjects != null ? 
            sessionObjects.stream().map(Object::toString).collect(Collectors.toSet()) :
            Set.of();
    }
    
    @Override
    public void refreshSession(String username, String sessionId) {
        try {
            // Extend user sessions set expiration
            redisTemplate.expire(USER_SESSIONS_KEY + username, SESSION_TIMEOUT);
            
            // Update session data with new last accessed time
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
                sessionData.put("lastAccessed", LocalDateTime.now().toString());
                
                String updatedSessionData = objectMapper.writeValueAsString(sessionData);
                redisTemplate.opsForValue().set(SESSION_DATA_KEY + sessionId, updatedSessionData, SESSION_TIMEOUT);
                
                // Extend token mapping expiration
                String jwtToken = (String) sessionData.get("jwtToken");
                if (jwtToken != null) {
                    redisTemplate.expire(SESSION_TOKEN_KEY + jwtToken, SESSION_TIMEOUT);
                }
            }
            
            log.debug("Refreshed session {} for user {}", sessionId, username);
        } catch (Exception e) {
            log.error("Error refreshing session {} for user {}", sessionId, username, e);
        }
    }
    
    @Override
    public void setSessionAttribute(String username, String sessionId, String key, Object value) {
        try {
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
                sessionData.put(key, value);
                
                String updatedSessionData = objectMapper.writeValueAsString(sessionData);
                redisTemplate.opsForValue().set(SESSION_DATA_KEY + sessionId, updatedSessionData, SESSION_TIMEOUT);
                
                log.debug("Set session attribute {} for session {} user {}", key, sessionId, username);
            }
        } catch (Exception e) {
            log.error("Error setting session attribute {} for session {} user {}", key, sessionId, username, e);
        }
    }
    
    @Override
    public Object getSessionAttribute(String username, String sessionId, String key) {
        try {
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
                return sessionData.get(key);
            }
        } catch (Exception e) {
            log.error("Error getting session attribute {} for session {} user {}", key, sessionId, username, e);
        }
        return null;
    }
    
    @Override
    public boolean hasActiveSessions(String username) {
        Long sessionCount = redisTemplate.opsForSet().size(USER_SESSIONS_KEY + username);
        return sessionCount != null && sessionCount > 0;
    }
    
    @Override
    public SessionInfo getSessionInfo(String username, String sessionId) {
        try {
            Object sessionDataObj = redisTemplate.opsForValue().get(SESSION_DATA_KEY + sessionId);
            if (sessionDataObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sessionData = objectMapper.readValue(sessionDataObj.toString(), Map.class);
                
                return new SessionInfo(
                    sessionId,
                    (String) sessionData.get("username"),
                    (String) sessionData.get("createdAt"),
                    (String) sessionData.get("lastAccessed"),
                    (String) sessionData.get("userAgent"),
                    (String) sessionData.get("ipAddress"),
                    (Boolean) sessionData.getOrDefault("active", false)
                );
            }
        } catch (Exception e) {
            log.error("Error getting session info for session {} user {}", sessionId, username, e);
        }
        return null;
    }
    
    @Override
    public void cleanupExpiredSessions() {
        // This method can be called periodically to clean up expired sessions
        // Redis TTL should handle most cleanup automatically, but this provides additional safety
        
        Set<String> allKeys = redisTemplate.keys(SESSION_DATA_KEY + "*");
        if (allKeys != null) {
            for (String key : allKeys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null && ttl <= 0) {
                    redisTemplate.delete(key);
                    log.debug("Cleaned up expired session key: {}", key);
                }
            }
        }
    }
}