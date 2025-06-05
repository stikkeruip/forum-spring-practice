package com.uipko.forumbackend.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.AuthenticationCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthenticationCacheServiceImpl implements AuthenticationCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    
    // Redis key patterns
    private static final String USER_DETAILS_KEY = "forum:auth:userdetails:";
    private static final String USER_ENTITY_KEY = "forum:auth:user:";
    private static final String USER_ROLES_KEY = "forum:auth:roles:";
    private static final String FAILED_LOGIN_KEY = "forum:auth:failed:";
    private static final String PASSWORD_RESET_KEY = "forum:auth:reset:";
    private static final String EMAIL_VERIFY_KEY = "forum:auth:verify:";
    private static final String TWO_FA_KEY = "forum:auth:2fa:";
    private static final String LAST_LOGIN_KEY = "forum:auth:lastlogin:";
    private static final String CACHE_STATS_KEY = "forum:auth:stats";
    
    // Cache timeouts
    private static final Duration USER_DETAILS_TTL = Duration.ofMinutes(15);
    private static final Duration USER_ENTITY_TTL = Duration.ofMinutes(10);
    private static final Duration FAILED_LOGIN_TTL = Duration.ofHours(1);
    private static final Duration LAST_LOGIN_TTL = Duration.ofDays(30);
    
    // Rate limiting configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    
    public AuthenticationCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, 
                                        ObjectMapper objectMapper, 
                                        UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }
    
    @Override
    public void cacheUserDetails(String username, UserDetails userDetails) {
        try {
            Map<String, Object> userDetailsMap = new HashMap<>();
            userDetailsMap.put("username", userDetails.getUsername());
            userDetailsMap.put("password", userDetails.getPassword());
            userDetailsMap.put("authorities", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList()));
            userDetailsMap.put("enabled", userDetails.isEnabled());
            userDetailsMap.put("accountNonExpired", userDetails.isAccountNonExpired());
            userDetailsMap.put("accountNonLocked", userDetails.isAccountNonLocked());
            userDetailsMap.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
            userDetailsMap.put("cachedAt", LocalDateTime.now().toString());
            
            String json = objectMapper.writeValueAsString(userDetailsMap);
            redisTemplate.opsForValue().set(USER_DETAILS_KEY + username, json, USER_DETAILS_TTL);
            
            incrementCacheRequest("userdetails");
            log.debug("Cached user details for: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache user details for: {}", username, e);
        }
    }
    
    @Override
    public Optional<UserDetails> getCachedUserDetails(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_DETAILS_KEY + username);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userDetailsMap = objectMapper.readValue(cached.toString(), Map.class);
                
                @SuppressWarnings("unchecked")
                List<String> authorities = (List<String>) userDetailsMap.get("authorities");
                
                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username((String) userDetailsMap.get("username"))
                    .password((String) userDetailsMap.get("password"))
                    .authorities(authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                    .accountExpired(!(Boolean) userDetailsMap.get("accountNonExpired"))
                    .accountLocked(!(Boolean) userDetailsMap.get("accountNonLocked"))
                    .credentialsExpired(!(Boolean) userDetailsMap.get("credentialsNonExpired"))
                    .disabled(!(Boolean) userDetailsMap.get("enabled"))
                    .build();
                
                incrementCacheHit("userdetails");
                log.debug("Retrieved cached user details for: {}", username);
                return Optional.of(userDetails);
            }
            
            incrementCacheMiss("userdetails");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve cached user details for: {}", username, e);
            incrementCacheMiss("userdetails");
            return Optional.empty();
        }
    }
    
    @Override
    public void cacheUser(String username, User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(USER_ENTITY_KEY + username, json, USER_ENTITY_TTL);
            
            incrementCacheRequest("userentity");
            log.debug("Cached user entity for: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache user entity for: {}", username, e);
        }
    }
    
    @Override
    public Optional<User> getCachedUser(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_ENTITY_KEY + username);
            if (cached != null) {
                User user = objectMapper.readValue(cached.toString(), User.class);
                incrementCacheHit("userentity");
                log.debug("Retrieved cached user entity for: {}", username);
                return Optional.of(user);
            }
            
            incrementCacheMiss("userentity");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve cached user entity for: {}", username, e);
            incrementCacheMiss("userentity");
            return Optional.empty();
        }
    }
    
    @Override
    public void cacheUserRoles(String username, Set<String> roles) {
        try {
            String json = objectMapper.writeValueAsString(roles);
            redisTemplate.opsForValue().set(USER_ROLES_KEY + username, json, USER_DETAILS_TTL);
            log.debug("Cached user roles for: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache user roles for: {}", username, e);
        }
    }
    
    @Override
    public Set<String> getCachedUserRoles(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(USER_ROLES_KEY + username);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Set<String> roles = objectMapper.readValue(cached.toString(), Set.class);
                log.debug("Retrieved cached user roles for: {}", username);
                return roles;
            }
            return Set.of();
        } catch (Exception e) {
            log.error("Failed to retrieve cached user roles for: {}", username, e);
            return Set.of();
        }
    }
    
    @Override
    public void recordFailedLoginAttempt(String username, String ipAddress) {
        String key = FAILED_LOGIN_KEY + username + ":" + ipAddress;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, FAILED_LOGIN_TTL);
        
        // Also track by username only
        String userKey = FAILED_LOGIN_KEY + username;
        redisTemplate.opsForValue().increment(userKey);
        redisTemplate.expire(userKey, FAILED_LOGIN_TTL);
        
        log.warn("Recorded failed login attempt for user: {} from IP: {}", username, ipAddress);
    }
    
    @Override
    public boolean isLoginBlocked(String username, String ipAddress) {
        String userIpKey = FAILED_LOGIN_KEY + username + ":" + ipAddress;
        String userKey = FAILED_LOGIN_KEY + username;
        
        Object userIpAttempts = redisTemplate.opsForValue().get(userIpKey);
        Object userAttempts = redisTemplate.opsForValue().get(userKey);
        
        int userIpFailures = userIpAttempts != null ? Integer.parseInt(userIpAttempts.toString()) : 0;
        int userFailures = userAttempts != null ? Integer.parseInt(userAttempts.toString()) : 0;
        
        boolean blocked = userIpFailures >= MAX_FAILED_ATTEMPTS || userFailures >= (MAX_FAILED_ATTEMPTS * 2);
        
        if (blocked) {
            log.warn("Login blocked for user: {} from IP: {} (userIp: {}, user: {})", 
                username, ipAddress, userIpFailures, userFailures);
        }
        
        return blocked;
    }
    
    @Override
    public void clearFailedLoginAttempts(String username) {
        Set<String> keys = redisTemplate.keys(FAILED_LOGIN_KEY + username + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Cleared failed login attempts for user: {}", username);
        }
    }
    
    @Override
    public void cachePasswordResetToken(String username, String token, Duration expiration) {
        Map<String, Object> tokenData = Map.of(
            "token", token,
            "username", username,
            "createdAt", LocalDateTime.now().toString()
        );
        
        try {
            String json = objectMapper.writeValueAsString(tokenData);
            redisTemplate.opsForValue().set(PASSWORD_RESET_KEY + username, json, expiration);
            log.debug("Cached password reset token for user: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache password reset token for: {}", username, e);
        }
    }
    
    @Override
    public boolean isValidPasswordResetToken(String username, String token) {
        try {
            Object cached = redisTemplate.opsForValue().get(PASSWORD_RESET_KEY + username);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenData = objectMapper.readValue(cached.toString(), Map.class);
                String cachedToken = (String) tokenData.get("token");
                return token.equals(cachedToken);
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to validate password reset token for: {}", username, e);
            return false;
        }
    }
    
    @Override
    public void invalidatePasswordResetToken(String username) {
        redisTemplate.delete(PASSWORD_RESET_KEY + username);
        log.debug("Invalidated password reset token for user: {}", username);
    }
    
    @Override
    public void cacheEmailVerificationToken(String username, String token, Duration expiration) {
        Map<String, Object> tokenData = Map.of(
            "token", token,
            "username", username,
            "createdAt", LocalDateTime.now().toString()
        );
        
        try {
            String json = objectMapper.writeValueAsString(tokenData);
            redisTemplate.opsForValue().set(EMAIL_VERIFY_KEY + username, json, expiration);
            log.debug("Cached email verification token for user: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache email verification token for: {}", username, e);
        }
    }
    
    @Override
    public boolean isValidEmailVerificationToken(String username, String token) {
        try {
            Object cached = redisTemplate.opsForValue().get(EMAIL_VERIFY_KEY + username);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenData = objectMapper.readValue(cached.toString(), Map.class);
                String cachedToken = (String) tokenData.get("token");
                return token.equals(cachedToken);
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to validate email verification token for: {}", username, e);
            return false;
        }
    }
    
    @Override
    public void cache2FACode(String username, String code, Duration expiration) {
        redisTemplate.opsForValue().set(TWO_FA_KEY + username, code, expiration);
        log.debug("Cached 2FA code for user: {}", username);
    }
    
    @Override
    public boolean isValid2FACode(String username, String code) {
        Object cached = redisTemplate.opsForValue().get(TWO_FA_KEY + username);
        boolean valid = cached != null && code.equals(cached.toString());
        if (valid) {
            // Invalidate code after successful use
            redisTemplate.delete(TWO_FA_KEY + username);
        }
        return valid;
    }
    
    @Override
    public void cacheLastLogin(String username, String ipAddress, String userAgent) {
        Map<String, Object> loginInfo = Map.of(
            "username", username,
            "ipAddress", ipAddress,
            "userAgent", userAgent != null ? userAgent : "",
            "timestamp", LocalDateTime.now().toString(),
            "successful", true
        );
        
        try {
            String json = objectMapper.writeValueAsString(loginInfo);
            redisTemplate.opsForValue().set(LAST_LOGIN_KEY + username, json, LAST_LOGIN_TTL);
            log.debug("Cached last login info for user: {}", username);
        } catch (JsonProcessingException e) {
            log.error("Failed to cache last login info for: {}", username, e);
        }
    }
    
    @Override
    public LastLoginInfo getLastLoginInfo(String username) {
        try {
            Object cached = redisTemplate.opsForValue().get(LAST_LOGIN_KEY + username);
            if (cached != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> loginData = objectMapper.readValue(cached.toString(), Map.class);
                return new LastLoginInfo(
                    (String) loginData.get("username"),
                    (String) loginData.get("ipAddress"),
                    (String) loginData.get("userAgent"),
                    (String) loginData.get("timestamp"),
                    (Boolean) loginData.get("successful")
                );
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to retrieve last login info for: {}", username, e);
            return null;
        }
    }
    
    @Override
    public void invalidateUserAuthCache(String username) {
        Set<String> keys = redisTemplate.keys("forum:auth:*:" + username);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Invalidated all auth cache for user: {}", username);
        }
    }
    
    @Override
    public void warmUpAuthenticationCache() {
        log.info("Starting authentication cache warm-up");
        
        // Cache frequently accessed users
        List<User> frequentUsers = userRepository.findAll(); // In production, limit this query
        
        for (User user : frequentUsers) {
            cacheUser(user.getName(), user);
            cacheUserRoles(user.getName(), Set.of(user.getRole()));
        }
        
        log.info("Authentication cache warm-up completed for {} users", frequentUsers.size());
    }
    
    @Override
    public AuthCacheStats getCacheStats() {
        try {
            Object statsObj = redisTemplate.opsForValue().get(CACHE_STATS_KEY);
            if (statsObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = objectMapper.readValue(statsObj.toString(), Map.class);
                
                long totalRequests = ((Number) stats.getOrDefault("totalRequests", 0)).longValue();
                long totalHits = ((Number) stats.getOrDefault("totalHits", 0)).longValue();
                double hitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
                
                return new AuthCacheStats(
                    getKeyCount(USER_DETAILS_KEY),
                    getKeyCount(USER_ENTITY_KEY),
                    getKeyCount(FAILED_LOGIN_KEY),
                    getKeyCount(PASSWORD_RESET_KEY),
                    hitRate,
                    totalRequests
                );
            }
        } catch (Exception e) {
            log.error("Failed to retrieve cache stats", e);
        }
        
        return new AuthCacheStats(0, 0, 0, 0, 0.0, 0);
    }
    
    private long getKeyCount(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern + "*");
        return keys != null ? keys.size() : 0;
    }
    
    private void incrementCacheRequest(String type) {
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "totalRequests", 1);
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, type + "Requests", 1);
        redisTemplate.expire(CACHE_STATS_KEY, Duration.ofDays(1));
    }
    
    private void incrementCacheHit(String type) {
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "totalHits", 1);
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, type + "Hits", 1);
    }
    
    private void incrementCacheMiss(String type) {
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, "totalMisses", 1);
        redisTemplate.opsForHash().increment(CACHE_STATS_KEY, type + "Misses", 1);
    }
}