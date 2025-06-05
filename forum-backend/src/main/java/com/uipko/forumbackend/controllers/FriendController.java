package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.RateLimitExceededException;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.FriendService;
import com.uipko.forumbackend.services.RateLimitingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/friends")
@PreAuthorize("isAuthenticated()")
public class FriendController {
    
    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);
    
    private final FriendService friendService;
    private final CurrentUserProvider currentUserProvider;
    private final RateLimitingService rateLimitingService;
    
    public FriendController(FriendService friendService, CurrentUserProvider currentUserProvider, RateLimitingService rateLimitingService) {
        this.friendService = friendService;
        this.currentUserProvider = currentUserProvider;
        this.rateLimitingService = rateLimitingService;
    }
    
    /**
     * Send a friend request to another user
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> sendFriendRequest(@Valid @RequestBody FriendRequestDto friendRequestDto) {
        try {
            logger.info("Starting sendFriendRequest");
            User currentUser = currentUserProvider.getAuthenticatedUser();
            logger.info("User {} sending friend request to {}", currentUser.getName(), friendRequestDto.getFriendUsername());
            
            friendService.sendFriendRequest(currentUser.getName(), friendRequestDto.getFriendUsername());
            
            return ResponseEntity.ok(Map.of("message", "Friend request sent successfully"));
        } catch (Exception e) {
            logger.error("Error in sendFriendRequest: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Respond to a friend request (accept or decline)
     */
    @PostMapping("/respond")
    public ResponseEntity<Map<String, String>> respondToFriendRequest(@Valid @RequestBody FriendResponseDto friendResponseDto) {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        logger.info("User {} responding to friend request {} with {}", 
                currentUser.getName(), friendResponseDto.getFriendshipId(), friendResponseDto.getResponse());
        
        friendService.respondToFriendRequest(currentUser.getName(), friendResponseDto.getFriendshipId(), friendResponseDto.getResponse());
        
        String message = "ACCEPT".equals(friendResponseDto.getResponse()) ? 
                "Friend request accepted" : "Friend request declined";
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Remove a friend (unfriend)
     */
    @DeleteMapping("/{friendUsername}")
    public ResponseEntity<Map<String, String>> removeFriend(@PathVariable String friendUsername) {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        logger.info("User {} removing friend {}", currentUser.getName(), friendUsername);
        
        friendService.removeFriend(currentUser.getName(), friendUsername);
        
        return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
    }
    
    /**
     * Block a user from sending friend requests
     */
    @PostMapping("/block/{username}")
    public ResponseEntity<Map<String, String>> blockUser(@PathVariable String username) {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        logger.info("User {} blocking user {}", currentUser.getName(), username);
        
        friendService.blockUser(currentUser.getName(), username);
        
        return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
    }
    
    /**
     * Unblock a user
     */
    @DeleteMapping("/block/{username}")
    public ResponseEntity<Map<String, String>> unblockUser(@PathVariable String username) {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        logger.info("User {} unblocking user {}", currentUser.getName(), username);
        
        friendService.unblockUser(currentUser.getName(), username);
        
        return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
    }
    
    /**
     * Get all friends for the current user
     */
    @GetMapping
    public ResponseEntity<List<FriendDto>> getFriends() {
        try {
            logger.info("Starting getFriends request");
            User currentUser = currentUserProvider.getAuthenticatedUser();
            
            // Rate limiting check
            if (!rateLimitingService.canMakeGetFriendsRequest(currentUser.getName())) {
                throw new RateLimitExceededException("getFriends", "Maximum 2 requests per minute allowed");
            }
            
            logger.info("User {} getting friends list", currentUser.getName());
            
            List<FriendDto> friends = friendService.getFriends(currentUser.getName());
            logger.info("Successfully retrieved {} friends for user {}", friends.size(), currentUser.getName());
            
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            logger.error("Error in getFriends: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get pending friend requests received by the current user
     */
    @GetMapping("/requests/received")
    public ResponseEntity<List<PendingFriendRequestDto>> getPendingRequests() {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        
        // Rate limiting check
        if (!rateLimitingService.canMakeGeneralRequest(currentUser.getName())) {
            throw new RateLimitExceededException("pendingRequests", "Maximum 10 requests per minute allowed");
        }
        
        logger.info("User {} getting pending friend requests", currentUser.getName());
        
        List<PendingFriendRequestDto> requests = friendService.getPendingRequests(currentUser.getName());
        
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Get pending friend requests sent by the current user
     */
    @GetMapping("/requests/sent")
    public ResponseEntity<List<PendingFriendRequestDto>> getSentRequests() {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        logger.info("User {} getting sent friend requests", currentUser.getName());
        
        List<PendingFriendRequestDto> requests = friendService.getSentRequests(currentUser.getName());
        
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Get friendship status with another user
     */
    @GetMapping("/status/{username}")
    public ResponseEntity<FriendshipStatusDto> getFriendshipStatus(@PathVariable String username) {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        
        // Rate limiting check
        if (!rateLimitingService.canMakeFriendshipStatusRequest(currentUser.getName())) {
            throw new RateLimitExceededException("friendshipStatus", "Maximum 5 requests per minute allowed");
        }
        
        logger.info("User {} checking friendship status with {}", currentUser.getName(), username);
        
        FriendshipStatusDto status = friendService.getFriendshipStatus(currentUser.getName(), username);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Get count of pending friend requests for the current user
     */
    @GetMapping("/requests/count")
    public ResponseEntity<Map<String, Long>> getPendingRequestsCount() {
        User currentUser = currentUserProvider.getAuthenticatedUser();
        
        long count = friendService.getPendingRequestsCount(currentUser.getName());
        
        return ResponseEntity.ok(Map.of("count", count));
    }
}