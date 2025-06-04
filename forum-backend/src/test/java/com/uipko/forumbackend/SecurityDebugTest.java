package com.uipko.forumbackend;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.CustomUserDetailsService;
import com.uipko.forumbackend.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SecurityDebugTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private RoleHierarchy roleHierarchy;

    @Autowired
    private JwtService jwtService;

    private User adminUser;

    @BeforeEach
    public void setUp() {
        SecurityContextHolder.clearContext();

        // Create admin user if not exists
        adminUser = userRepository.findUserByName("testadmin").orElseGet(() -> {
            User user = new User();
            user.setName("testadmin");
            user.setPassword("$2a$12$eGHQWHZTJ/Qa9Vqb3JBxKuQxZ1/KBwPbvZ7XP8aBUiRJp9f4SqJAG");
            user.setCreatedDate(LocalDateTime.now());
            user.setRole("ADMIN");
            return userRepository.save(user);
        });
    }

    @Test
    public void testAdminUserDetailsAuthorities() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("testadmin");
        
        System.out.println("UserDetails for admin user:");
        System.out.println("Username: " + userDetails.getUsername());
        System.out.println("Authorities: " + userDetails.getAuthorities());
        
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    public void testRoleHierarchy() {
        Collection<? extends GrantedAuthority> adminAuthorities = 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        Collection<? extends GrantedAuthority> reachableAuthorities = 
            roleHierarchy.getReachableGrantedAuthorities(adminAuthorities);
        
        System.out.println("Role hierarchy for ADMIN:");
        System.out.println("Direct authorities: " + adminAuthorities);
        System.out.println("Reachable authorities: " + reachableAuthorities);
        
        // Admin should have ADMIN, MODERATOR, and USER roles through hierarchy
        assertTrue(reachableAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(reachableAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_MODERATOR")));
        assertTrue(reachableAuthorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    }

    @Test
    public void testSecurityContextWithAdmin() {
        // Authenticate as admin
        UserDetails userDetails = userDetailsService.loadUserByUsername("testadmin");
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Security context authentication:");
        System.out.println("Principal: " + auth.getPrincipal());
        System.out.println("Authorities: " + auth.getAuthorities());
        System.out.println("Is authenticated: " + auth.isAuthenticated());
        
        // Check if hasRole works
        boolean hasAdminRole = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        assertTrue(hasAdminRole);
    }

    @Test
    public void testJwtTokenGeneration() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("testadmin");
        String token = jwtService.generateToken(userDetails.getUsername(), adminUser.getRole());
        
        System.out.println("JWT Token for admin: " + token);
        
        String extractedUsername = jwtService.getUserName(token);
        assertEquals("testadmin", extractedUsername);
        
        String extractedRole = jwtService.getRole(token);
        assertEquals("ADMIN", extractedRole);
        
        assertTrue(jwtService.validateToken(token, userDetails));
    }
}