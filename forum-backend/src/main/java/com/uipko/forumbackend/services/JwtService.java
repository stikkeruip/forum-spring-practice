package com.uipko.forumbackend.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private final SessionManagementService sessionManagementService;

    @Value("${jwt.secret}")
    private String SECRET_KEY;
    
    public JwtService(SessionManagementService sessionManagementService) {
        this.sessionManagementService = sessionManagementService;
    }

    public String generateToken(String name, String role) {
        logger.info("Generating JWT token for user: {} with role: {}", name, role);
        
        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("sessionId", sessionId);

        String token = Jwts.builder()
                .claims()
                .add(claims)
                .subject(name)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 10 * 1000))
                .and()
                .signWith(getKey())
                .compact();
        
        // Store session in Redis
        sessionManagementService.createUserSession(name, sessionId, token);
        
        return token;
    }

    private SecretKey getKey() {
        byte[] keyByte = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyByte);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserName(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = getUserName(token);
        boolean isTokenValid = (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
        
        if (!isTokenValid) {
            logger.warn("Invalid token validation for user: {}", userDetails.getUsername());
            return false;
        }
        
        // Check if session is still active in Redis
        boolean isSessionValid = sessionManagementService.isValidSession(userName, token);
        if (!isSessionValid) {
            logger.warn("Token valid but session expired for user: {}", userDetails.getUsername());
            return false;
        }
        
        return true;
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public String getRole(String token) {
        return extractClaims(token, claims -> claims.get("role", String.class));
    }
    
    public String getSessionId(String token) {
        return extractClaims(token, claims -> claims.get("sessionId", String.class));
    }
    
    /**
     * Invalidate a specific token/session
     */
    public void invalidateToken(String token) {
        try {
            String username = getUserName(token);
            String sessionId = getSessionId(token);
            
            if (username != null && sessionId != null) {
                sessionManagementService.invalidateSession(username, sessionId);
                logger.info("Invalidated session {} for user {}", sessionId, username);
            }
        } catch (Exception e) {
            logger.error("Error invalidating token", e);
        }
    }
    
    /**
     * Invalidate all sessions for a user (logout from all devices)
     */
    public void invalidateAllUserTokens(String username) {
        sessionManagementService.invalidateAllUserSessions(username);
        logger.info("Invalidated all sessions for user {}", username);
    }
}
