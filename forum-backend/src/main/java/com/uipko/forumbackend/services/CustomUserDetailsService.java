package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.UserNameNotFoundException;
import com.uipko.forumbackend.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthenticationCacheService authCacheService;

    public CustomUserDetailsService(UserRepository userRepository, AuthenticationCacheService authCacheService) {
        this.userRepository = userRepository;
        this.authCacheService = authCacheService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        log.debug("Loading user by username: {}", username);
        
        // Try to get from cache first
        Optional<UserDetails> cachedUserDetails = authCacheService.getCachedUserDetails(username);
        if (cachedUserDetails.isPresent()) {
            log.debug("Retrieved user details from cache for: {}", username);
            return cachedUserDetails.get();
        }
        
        // Load from database if not in cache
        User user = userRepository.findUserByName(username)
            .orElseThrow(() -> new UserNameNotFoundException(username));
        
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getName(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        
        // Cache the user details and entity
        authCacheService.cacheUserDetails(username, userDetails);
        authCacheService.cacheUser(username, user);
        authCacheService.cacheUserRoles(username, Set.of(user.getRole()));
        
        log.debug("Loaded and cached user details for: {}", username);
        return userDetails;
    }
}
