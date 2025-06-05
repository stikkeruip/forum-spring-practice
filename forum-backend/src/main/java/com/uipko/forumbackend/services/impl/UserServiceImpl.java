package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.IncorrectLoginException;
import com.uipko.forumbackend.exceptions.UserNameEmptyException;
import com.uipko.forumbackend.exceptions.UserNameExistsException;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.JwtService;
import com.uipko.forumbackend.services.UserService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTemplate<String, Object> redisTemplate;

    public UserServiceImpl(UserRepository userRepository,
                           AuthenticationManager authenticationManager,
                           BCryptPasswordEncoder passwordEncoder, JwtService jwtService,
                           RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Cacheable(value = "users", key = "'user:' + #name", unless = "#result == null")
    public User getUser(String name) {
        log.debug("Cache miss - fetching user {} from database", name);
        return userRepository.findUserByName(name).orElseThrow(() -> new UserNotFoundException(name));
    }

    @Transactional
    @Override
    public User registerUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new UserNameEmptyException();
        }
        
        // Check username existence (this will be cached)
        if (checkUsernameExists(user.getName())) {
            throw new UserNameExistsException(user.getName());
        }

        User newUser = new User();
        newUser.setName(user.getName());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setCreatedDate(user.getCreatedDate());
        newUser.setRole("USER");
        newUser.setIsOnline(false);
        newUser.setLastSeen(user.getCreatedDate());
        
        User savedUser = userRepository.save(newUser);
        
        // Cache the new user
        redisTemplate.opsForValue().set(
            "users::user:" + savedUser.getName(),
            savedUser,
            Duration.ofMinutes(30)
        );
        
        // Update username existence cache
        redisTemplate.opsForValue().set(
            "users::exists:" + savedUser.getName(),
            true,
            Duration.ofHours(24)
        );
        
        log.info("Registered new user: {}", savedUser.getName());
        return savedUser;
    }

    @Override
    public String loginUser(User user) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getName(), user.getPassword())
            );
            // Fetch the user from cache/database to get their role
            User authenticatedUser = getUser(user.getName()); // This will use cache
            
            // Create user session
            createUserSession(authenticatedUser.getName());
            
            log.info("User {} logged in successfully", user.getName());
            return jwtService.generateToken(user.getName(), authenticatedUser.getRole());
        } catch (AuthenticationException ex) {
            throw new IncorrectLoginException();
        }
    }

    @Transactional
    @Override
    @CacheEvict(value = "users", key = "'user:' + #userName")
    public void deleteUser(String userName) {
        userRepository.delete(getUser(userName));
        
        // Clear related caches
        redisTemplate.delete("users::exists:" + userName);
        redisTemplate.delete("users::sessions:" + userName);
        
        log.info("Deleted user: {}", userName);
    }

    /**
     * Check if username exists - cached method
     */
    @Cacheable(value = "users", key = "'exists:' + #username")
    public boolean checkUsernameExists(String username) {
        log.debug("Cache miss - checking username existence for: {}", username);
        return userRepository.existsByName(username);
    }

    /**
     * Create user session for tracking
     */
    public void createUserSession(String username) {
        String sessionKey = "users:session:" + username;
        String sessionData = LocalDateTime.now().toString();
        
        redisTemplate.opsForValue().set(
            sessionKey,
            sessionData,
            Duration.ofHours(24)
        );
        
        // Track user in recently active users for cache warming
        redisTemplate.opsForSet().add("recently_online_users", username);
        redisTemplate.expire("recently_online_users", Duration.ofHours(1));
        
        log.debug("Created session for user: {}", username);
    }

    /**
     * JWT Token blacklisting for logout
     */
    public void blacklistToken(String tokenId, LocalDateTime expiration) {
        String blacklistKey = "users:blacklist:token:" + tokenId;
        redisTemplate.opsForValue().set(
            blacklistKey,
            "BLACKLISTED",
            Duration.between(LocalDateTime.now(), expiration)
        );
        log.debug("Blacklisted token: {}", tokenId);
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String blacklistKey = "users:blacklist:token:" + tokenId;
        return redisTemplate.hasKey(blacklistKey);
    }

    /**
     * Invalidate user cache
     */
    @CacheEvict(value = "users", key = "'user:' + #username")
    public void invalidateUserCache(String username) {
        log.info("Invalidating cache for user: {}", username);
    }
}
