package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.dto.*;

import java.util.List;

public interface FriendService {
    
    /**
     * Send a friend request to another user.
     */
    void sendFriendRequest(String requesterUsername, String addresseeUsername);
    
    /**
     * Respond to a friend request (accept or decline).
     */
    void respondToFriendRequest(String addresseeUsername, Long friendshipId, String response);
    
    /**
     * Remove a friend (unfriend).
     */
    void removeFriend(String username, String friendUsername);
    
    /**
     * Block a user from sending friend requests.
     */
    void blockUser(String username, String blockedUsername);
    
    /**
     * Unblock a user.
     */
    void unblockUser(String username, String unblockedUsername);
    
    /**
     * Get all friends for a user, sorted by online status first, then by name.
     */
    List<FriendDto> getFriends(String username);
    
    /**
     * Get pending friend requests received by a user.
     */
    List<PendingFriendRequestDto> getPendingRequests(String username);
    
    /**
     * Get pending friend requests sent by a user.
     */
    List<PendingFriendRequestDto> getSentRequests(String username);
    
    /**
     * Get friendship status between two users.
     */
    FriendshipStatusDto getFriendshipStatus(String username, String otherUsername);
    
    /**
     * Get count of pending friend requests for a user.
     */
    long getPendingRequestsCount(String username);
}