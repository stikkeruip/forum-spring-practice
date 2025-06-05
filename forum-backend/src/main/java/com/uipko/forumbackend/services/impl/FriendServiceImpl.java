package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Friendship;
import com.uipko.forumbackend.domain.entities.Friendship.FriendshipStatus;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.*;
import com.uipko.forumbackend.repositories.FriendshipRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.FriendService;
import com.uipko.forumbackend.services.NotificationService;
import com.uipko.forumbackend.controllers.WebSocketController;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {
    
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WebSocketController webSocketController;
    
    public FriendServiceImpl(FriendshipRepository friendshipRepository,
                           UserRepository userRepository,
                           NotificationService notificationService,
                           WebSocketController webSocketController) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.webSocketController = webSocketController;
    }
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friends", key = "#requesterUsername"),
            @CacheEvict(value = "friends", key = "#addresseeUsername"),
            @CacheEvict(value = "pendingRequests", key = "#addresseeUsername"),
            @CacheEvict(value = "sentRequests", key = "#requesterUsername"),
            @CacheEvict(value = "pendingRequestsCount", key = "#addresseeUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#requesterUsername + '_' + #addresseeUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#addresseeUsername + '_' + #requesterUsername")
    })
    @Override
    public void sendFriendRequest(String requesterUsername, String addresseeUsername) {
        // Validate input
        if (requesterUsername.equals(addresseeUsername)) {
            throw new SelfFriendRequestException();
        }
        
        // Get users
        User requester = userRepository.findByName(requesterUsername)
                .orElseThrow(() -> new UserNotFoundException(requesterUsername));
        User addressee = userRepository.findByName(addresseeUsername)
                .orElseThrow(() -> new UserNotFoundException(addresseeUsername));
        
        // Check if friendship already exists
        long existingCount = friendshipRepository.countExistingFriendshipOrRequest(requesterUsername, addresseeUsername, 
                FriendshipStatus.PENDING, FriendshipStatus.ACCEPTED, FriendshipStatus.BLOCKED);
        if (existingCount > 0) {
            throw new FriendRequestAlreadyExistsException(requesterUsername, addresseeUsername);
        }
        
        // Create friendship
        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();
        
        friendshipRepository.save(friendship);
        
        // Create notification for friend request
        createFriendRequestNotification(requester, addressee);
        
        // Send WebSocket notifications
        webSocketController.sendFriendshipUpdate(requesterUsername, "request_sent", addresseeUsername);
        webSocketController.sendFriendshipUpdate(addresseeUsername, "request_received", requesterUsername);
    }
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friends", allEntries = true),
            @CacheEvict(value = "pendingRequests", allEntries = true),
            @CacheEvict(value = "sentRequests", allEntries = true),
            @CacheEvict(value = "pendingRequestsCount", allEntries = true),
            @CacheEvict(value = "friendshipStatus", allEntries = true)
    })
    @Override
    public void respondToFriendRequest(String addresseeUsername, Long friendshipId, String response) {
        // Validate response
        if (!"ACCEPT".equals(response) && !"DECLINE".equals(response)) {
            throw new InvalidFriendResponseException(response);
        }
        
        // Get friendship
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipNotFoundException(friendshipId));
        
        // Verify user is the addressee
        if (!friendship.getAddressee().getName().equals(addresseeUsername)) {
            throw new FriendshipNotFoundException(friendshipId);
        }
        
        // Verify friendship is pending
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendRequestAlreadyExistsException(friendship.getRequester().getName(), addresseeUsername);
        }
        
        // Update friendship status
        FriendshipStatus newStatus = "ACCEPT".equals(response) ? FriendshipStatus.ACCEPTED : FriendshipStatus.DECLINED;
        friendship.setStatus(newStatus);
        friendshipRepository.save(friendship);
        
        // Create notification for response
        if (newStatus == FriendshipStatus.ACCEPTED) {
            createFriendRequestAcceptedNotification(friendship.getAddressee(), friendship.getRequester());
            // Send WebSocket notifications for acceptance
            webSocketController.sendFriendshipUpdate(addresseeUsername, "accepted", friendship.getRequester().getName());
            webSocketController.sendFriendshipUpdate(friendship.getRequester().getName(), "accepted", addresseeUsername);
            webSocketController.sendFriendsListUpdate(addresseeUsername);
            webSocketController.sendFriendsListUpdate(friendship.getRequester().getName());
        } else {
            createFriendRequestDeclinedNotification(friendship.getAddressee(), friendship.getRequester());
            // Send WebSocket notifications for decline
            webSocketController.sendFriendshipUpdate(friendship.getRequester().getName(), "declined", addresseeUsername);
        }
    }
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friends", key = "#username"),
            @CacheEvict(value = "friends", key = "#friendUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#username + '_' + #friendUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#friendUsername + '_' + #username")
    })
    @Override
    public void removeFriend(String username, String friendUsername) {
        Friendship friendship = friendshipRepository.findFriendshipBetweenUsers(username, friendUsername)
                .orElseThrow(() -> new FriendshipNotFoundException(username, friendUsername));
        
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new FriendshipNotFoundException(username, friendUsername);
        }
        
        friendshipRepository.delete(friendship);
        
        // Send WebSocket notifications for friend removal
        webSocketController.sendFriendshipUpdate(username, "removed", friendUsername);
        webSocketController.sendFriendshipUpdate(friendUsername, "removed", username);
        webSocketController.sendFriendsListUpdate(username);
        webSocketController.sendFriendsListUpdate(friendUsername);
    }
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friends", key = "#username"),
            @CacheEvict(value = "friends", key = "#blockedUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#username + '_' + #blockedUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#blockedUsername + '_' + #username")
    })
    @Override
    public void blockUser(String username, String blockedUsername) {
        if (username.equals(blockedUsername)) {
            throw new SelfFriendRequestException();
        }
        
        User user = userRepository.findByName(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        User blockedUser = userRepository.findByName(blockedUsername)
                .orElseThrow(() -> new UserNotFoundException(blockedUsername));
        
        Optional<Friendship> existingFriendship = friendshipRepository.findFriendshipBetweenUsers(username, blockedUsername);
        
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            friendship.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(friendship);
        } else {
            // Create a new blocked relationship
            Friendship friendship = Friendship.builder()
                    .requester(user)
                    .addressee(blockedUser)
                    .status(FriendshipStatus.BLOCKED)
                    .build();
            friendshipRepository.save(friendship);
        }
    }
    
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "friendshipStatus", key = "#username + '_' + #unblockedUsername"),
            @CacheEvict(value = "friendshipStatus", key = "#unblockedUsername + '_' + #username")
    })
    @Override
    public void unblockUser(String username, String unblockedUsername) {
        Friendship friendship = friendshipRepository.findFriendshipBetweenUsers(username, unblockedUsername)
                .orElseThrow(() -> new FriendshipNotFoundException(username, unblockedUsername));
        
        if (friendship.getStatus() != FriendshipStatus.BLOCKED) {
            throw new FriendshipNotFoundException(username, unblockedUsername);
        }
        
        friendshipRepository.delete(friendship);
    }
    
    @Override
    @Cacheable(value = "friends", key = "#username")
    public List<FriendDto> getFriends(String username) {
        User currentUser = userRepository.findByName(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendsWithOnlineStatus(username, FriendshipStatus.ACCEPTED);
        
        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getOtherUser(currentUser);
                    return new FriendDto(
                            friend.getName(),
                            friend.getCreatedDate(),
                            friend.getIsOnline() != null ? friend.getIsOnline() : false,
                            friend.getLastSeen(),
                            friendship.getUpdatedDate()
                    );
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "pendingRequests", key = "#username")
    public List<PendingFriendRequestDto> getPendingRequests(String username) {
        List<Friendship> pendingRequests = friendshipRepository.findPendingRequestsForUser(username, FriendshipStatus.PENDING);
        
        return pendingRequests.stream()
                .map(friendship -> new PendingFriendRequestDto(
                        friendship.getId(),
                        friendship.getRequester().getName(),
                        friendship.getCreatedDate(),
                        friendship.getRequester().getIsOnline()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "sentRequests", key = "#username")
    public List<PendingFriendRequestDto> getSentRequests(String username) {
        List<Friendship> sentRequests = friendshipRepository.findPendingRequestsByUser(username, FriendshipStatus.PENDING);
        
        return sentRequests.stream()
                .map(friendship -> new PendingFriendRequestDto(
                        friendship.getId(),
                        friendship.getAddressee().getName(),
                        friendship.getCreatedDate(),
                        friendship.getAddressee().getIsOnline()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "friendshipStatus", key = "#username + '_' + #otherUsername")
    public FriendshipStatusDto getFriendshipStatus(String username, String otherUsername) {
        if (username.equals(otherUsername)) {
            return new FriendshipStatusDto("SELF", null, false);
        }
        
        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetweenUsers(username, otherUsername);
        
        if (friendship.isEmpty()) {
            return new FriendshipStatusDto("NONE", null, true);
        }
        
        Friendship f = friendship.get();
        String status;
        boolean canSendRequest = false;
        
        switch (f.getStatus()) {
            case PENDING:
                if (f.getRequester().getName().equals(username)) {
                    status = "PENDING_SENT";
                } else {
                    status = "PENDING_RECEIVED";
                }
                break;
            case ACCEPTED:
                status = "ACCEPTED";
                break;
            case DECLINED:
                status = "DECLINED";
                canSendRequest = true;
                break;
            case BLOCKED:
                status = "BLOCKED";
                break;
            default:
                status = "NONE";
                canSendRequest = true;
        }
        
        return new FriendshipStatusDto(status, f.getId(), canSendRequest);
    }
    
    @Override
    @Cacheable(value = "pendingRequestsCount", key = "#username")
    public long getPendingRequestsCount(String username) {
        return friendshipRepository.findPendingRequestsForUser(username, FriendshipStatus.PENDING).size();
    }
    
    private void createFriendRequestNotification(User requester, User addressee) {
        notificationService.createFriendRequestNotification(requester, addressee);
    }
    
    private void createFriendRequestAcceptedNotification(User accepter, User requester) {
        notificationService.createFriendRequestAcceptedNotification(accepter, requester);
    }
    
    private void createFriendRequestDeclinedNotification(User decliner, User requester) {
        notificationService.createFriendRequestDeclinedNotification(decliner, requester);
    }
}