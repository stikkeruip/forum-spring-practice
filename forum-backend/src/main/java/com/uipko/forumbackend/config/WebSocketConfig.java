package com.uipko.forumbackend.config;

import com.uipko.forumbackend.security.JwtAuthenticationFilter;
import com.uipko.forumbackend.services.JwtService;
import com.uipko.forumbackend.services.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple memory-based message broker for topics and queues
        config.enableSimpleBroker("/topic", "/queue");
        
        // Set prefix for messages bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                .withSockJS();
        
        // Also register native WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract JWT token from connection headers
                    String authToken = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authToken != null && authToken.startsWith("Bearer ")) {
                        String token = authToken.substring(7);
                        
                        try {
                            String username = jwtService.getUserName(token);
                            
                            if (username != null && !jwtService.isTokenExpired(token)) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                UsernamePasswordAuthenticationToken authenticationToken =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities()
                                        );
                                
                                // Set the authentication in the StompHeaderAccessor
                                accessor.setUser(authenticationToken);
                                
                                log.info("WebSocket connection authenticated for user: {}", username);
                            } else {
                                if (username == null) {
                                    log.warn("JWT token contains no username for WebSocket connection");
                                    throw new IllegalArgumentException("Invalid JWT token: no username found");
                                } else {
                                    log.warn("Expired JWT token for WebSocket connection from user: {}", username);
                                    throw new IllegalArgumentException("JWT token expired");
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            // Re-throw our specific errors
                            throw e;
                        } catch (Exception e) {
                            log.error("Error authenticating WebSocket connection: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                            
                            // Provide more specific error messages
                            if (e.getMessage().contains("expired")) {
                                throw new IllegalArgumentException("Authentication token expired");
                            } else if (e.getMessage().contains("malformed") || e.getMessage().contains("invalid")) {
                                throw new IllegalArgumentException("Malformed authentication token");
                            } else if (e.getMessage().contains("signature")) {
                                throw new IllegalArgumentException("Invalid token signature");
                            } else {
                                throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                            }
                        }
                    } else {
                        log.warn("No Authorization header found in WebSocket connection");
                        throw new IllegalArgumentException("Missing authentication token");
                    }
                }
                
                return message;
            }
        });
    }
}