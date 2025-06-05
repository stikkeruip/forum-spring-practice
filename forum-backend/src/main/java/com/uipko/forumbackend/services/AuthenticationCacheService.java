package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Service for caching authentication-related data across distributed application instances
 * Provides fast access to user details, permissions, and authentication state
 */
public interface AuthenticationCacheService {
    
    /**
     * Cache user details for fast authentication
     */
    void cacheUserDetails(String username, UserDetails userDetails);
    
    /**
     * Get cached user details
     */
    Optional<UserDetails> getCachedUserDetails(String username);
    
    /**
     * Cache user entity for profile operations
     */
    void cacheUser(String username, User user);
    
    /**
     * Get cached user entity
     */
    Optional<User> getCachedUser(String username);
    
    /**
     * Cache user permissions/roles
     */
    void cacheUserRoles(String username, Set<String> roles);
    
    /**
     * Get cached user roles
     */
    Set<String> getCachedUserRoles(String username);
    
    /**
     * Cache failed login attempts for rate limiting
     */
    void recordFailedLoginAttempt(String username, String ipAddress);
    
    /**
     * Check if user/IP has too many failed attempts
     */
    boolean isLoginBlocked(String username, String ipAddress);
    
    /**
     * Clear failed login attempts after successful login
     */
    void clearFailedLoginAttempts(String username);
    
    /**
     * Cache password reset tokens
     */
    void cachePasswordResetToken(String username, String token, Duration expiration);
    
    /**
     * Validate password reset token
     */
    boolean isValidPasswordResetToken(String username, String token);
    
    /**
     * Invalidate password reset token
     */
    void invalidatePasswordResetToken(String username);
    
    /**
     * Cache email verification tokens
     */
    void cacheEmailVerificationToken(String username, String token, Duration expiration);
    
    /**
     * Validate email verification token
     */
    boolean isValidEmailVerificationToken(String username, String token);
    
    /**
     * Cache two-factor authentication codes
     */
    void cache2FACode(String username, String code, Duration expiration);
    
    /**
     * Validate 2FA code
     */
    boolean isValid2FACode(String username, String code);
    
    /**
     * Cache user's last login information
     */
    void cacheLastLogin(String username, String ipAddress, String userAgent);
    
    /**
     * Get user's last login information
     */
    LastLoginInfo getLastLoginInfo(String username);
    
    /**
     * Invalidate all authentication cache for a user
     */
    void invalidateUserAuthCache(String username);
    
    /**
     * Warm up cache with frequently accessed users
     */
    void warmUpAuthenticationCache();
    
    /**
     * Get authentication cache statistics
     */
    AuthCacheStats getCacheStats();
    
    /**
     * Last login information
     */
    record LastLoginInfo(
        String username,
        String ipAddress,
        String userAgent,
        String timestamp,
        boolean successful
    ) {}
    
    /**
     * Authentication cache statistics
     */
    record AuthCacheStats(
        long userDetailsCacheSize,
        long userEntityCacheSize,
        long failedLoginAttempts,
        long activePasswordResetTokens,
        double cacheHitRate,
        long totalCacheRequests
    ) {}
}