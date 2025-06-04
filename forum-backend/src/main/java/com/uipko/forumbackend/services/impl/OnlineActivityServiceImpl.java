package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.dto.OnlineUserDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.OnlineActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineActivityServiceImpl implements OnlineActivityService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Users are considered offline if no activity for 5 minutes
    private static final int OFFLINE_THRESHOLD_MINUTES = 5;
    
    // Performance optimization: Batching and rate limiting
    private final Set<String> pendingStatusUpdates = new ConcurrentSkipListSet<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastHeartbeat = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastStatusBroadcast = new ConcurrentHashMap<>();
    
    // Rate limiting constants
    private static final int HEARTBEAT_THROTTLE_SECONDS = 30; // Minimum 30s between heartbeat updates
    private static final int STATUS_BROADCAST_THROTTLE_SECONDS = 5; // Minimum 5s between status broadcasts
    private static final int MAX_ONLINE_USERS_BROADCAST = 100; // Limit online users list size

    @Override
    @Transactional
    public void setUserOnline(String username) {
        User user = userRepository.findByName(username).orElse(null);
        if (user != null) {
            LocalDateTime now = LocalDateTime.now();
            boolean shouldUpdate = shouldUpdateUserActivity(username, now);
            
            if (shouldUpdate) {
                user.setIsOnline(true);
                user.setLastSeen(now);
                userRepository.save(user);
                
                log.debug("User {} set as online", username);
                
                // Add to pending status updates for batched broadcast
                pendingStatusUpdates.add(username + ":online");
                
                // Throttled broadcast
                if (shouldBroadcastStatus(username, now)) {
                    broadcastUserStatusChange(username, true);
                    broadcastOnlineUserCount(); // Also broadcast updated count
                    broadcastOnlineUsers(); // Also broadcast updated users list
                    lastStatusBroadcast.put(username, now);
                }
            }
            
            // Update heartbeat tracking
            lastHeartbeat.put(username, now);
        }
    }

    @Override
    @Transactional
    public void setUserOffline(String username) {
        User user = userRepository.findByName(username).orElse(null);
        if (user != null) {
            user.setIsOnline(false);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("User {} set as offline", username);
            
            // Broadcast user offline status to all connected clients
            broadcastUserStatusChange(username, false);
            broadcastOnlineUserCount();
            broadcastOnlineUsers();
        }
    }

    @Override
    @Transactional
    public void updateLastSeen(String username) {
        LocalDateTime now = LocalDateTime.now();
        
        // Throttle heartbeat updates to prevent excessive database writes
        if (!shouldUpdateUserActivity(username, now)) {
            return; // Skip this update
        }
        
        User user = userRepository.findByName(username).orElse(null);
        if (user != null) {
            user.setLastSeen(now);
            if (!Boolean.TRUE.equals(user.getIsOnline())) {
                user.setIsOnline(true);
                userRepository.save(user);
                
                // Broadcast status change if user was previously offline
                if (shouldBroadcastStatus(username, now)) {
                    broadcastUserStatusChange(username, true);
                    broadcastOnlineUserCount(); // Also broadcast updated count
                    broadcastOnlineUsers(); // Also broadcast updated users list
                    lastStatusBroadcast.put(username, now);
                }
            } else {
                userRepository.save(user);
            }
            
            // Update heartbeat tracking
            lastHeartbeat.put(username, now);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnlineUserDto> getOnlineUsers() {
        List<User> onlineUsers = userRepository.findByIsOnlineTrue();
        return onlineUsers.stream()
                .limit(MAX_ONLINE_USERS_BROADCAST) // Limit for performance
                .map(userMapper::userToOnlineUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getOnlineUserCount() {
        return userRepository.countByIsOnlineTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserOnline(String username) {
        return userRepository.findByName(username)
                .map(user -> Boolean.TRUE.equals(user.getIsOnline()))
                .orElse(false);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupStaleUsers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES);
        List<User> staleUsers = userRepository.findByIsOnlineTrueAndLastSeenBefore(cutoffTime);
        
        if (!staleUsers.isEmpty()) {
            // Batch update for better performance
            for (User user : staleUsers) {
                user.setIsOnline(false);
            }
            userRepository.saveAll(staleUsers); // Batch save
            
            // Batch broadcast status changes
            for (User user : staleUsers) {
                broadcastUserStatusChange(user.getName(), false);
            }
            
            broadcastOnlineUserCount();
            broadcastOnlineUsers();
            log.info("Cleaned up {} stale users", staleUsers.size());
        }
        
        // Clean up old tracking data to prevent memory leaks
        cleanupOldTrackingData();
    }
    
    /**
     * Batch process pending status updates (could be used for further optimization)
     */
    @Scheduled(fixedRate = 5000) // Run every 5 seconds
    public void processPendingUpdates() {
        if (!pendingStatusUpdates.isEmpty()) {
            // Process batched status updates
            Set<String> updates = new ConcurrentSkipListSet<>(pendingStatusUpdates);
            pendingStatusUpdates.clear();
            
            // Could implement batched broadcasting here if needed
            log.debug("Processed {} pending status updates", updates.size());
        }
    }
    
    /**
     * Clean up old tracking data to prevent memory leaks
     */
    private void cleanupOldTrackingData() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        lastHeartbeat.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
        lastStatusBroadcast.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
        
        log.debug("Cleaned up old tracking data");
    }
    
    /**
     * Check if user activity should be updated (throttling)
     */
    private boolean shouldUpdateUserActivity(String username, LocalDateTime now) {
        LocalDateTime lastUpdate = lastHeartbeat.get(username);
        return lastUpdate == null || 
               now.minusSeconds(HEARTBEAT_THROTTLE_SECONDS).isAfter(lastUpdate);
    }
    
    /**
     * Check if status broadcast should be sent (throttling)
     */
    private boolean shouldBroadcastStatus(String username, LocalDateTime now) {
        LocalDateTime lastBroadcast = lastStatusBroadcast.get(username);
        return lastBroadcast == null || 
               now.minusSeconds(STATUS_BROADCAST_THROTTLE_SECONDS).isAfter(lastBroadcast);
    }

    private void broadcastUserStatusChange(String username, boolean isOnline) {
        try {
            UserStatusDto statusDto = new UserStatusDto(username, isOnline);
            messagingTemplate.convertAndSend("/topic/user-status", statusDto);
        } catch (Exception e) {
            log.error("Error broadcasting user status change for {}: {}", username, e.getMessage());
        }
    }

    private long lastBroadcastCount = -1;
    private LocalDateTime lastCountBroadcast = LocalDateTime.MIN;
    private LocalDateTime lastUsersBroadcast = LocalDateTime.MIN;
    
    private void broadcastOnlineUserCount() {
        try {
            LocalDateTime now = LocalDateTime.now();
            long count = getOnlineUserCount();
            
            // Only broadcast if count changed or enough time has passed
            if (count != lastBroadcastCount || 
                now.minusSeconds(STATUS_BROADCAST_THROTTLE_SECONDS).isAfter(lastCountBroadcast)) {
                
                OnlineCountDto countDto = new OnlineCountDto(count);
                messagingTemplate.convertAndSend("/topic/online-count", countDto);
                
                lastBroadcastCount = count;
                lastCountBroadcast = now;
            }
        } catch (Exception e) {
            log.error("Error broadcasting online user count: {}", e.getMessage());
        }
    }
    
    private void broadcastOnlineUsers() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Throttle users list broadcasts to prevent excessive traffic
            if (now.minusSeconds(STATUS_BROADCAST_THROTTLE_SECONDS).isAfter(lastUsersBroadcast)) {
                List<OnlineUserDto> onlineUsers = getOnlineUsers();
                messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
                
                lastUsersBroadcast = now;
                log.debug("Broadcasted online users list with {} users", onlineUsers.size());
            }
        } catch (Exception e) {
            log.error("Error broadcasting online users list: {}", e.getMessage());
        }
    }

    // DTOs for WebSocket messages
    public static class UserStatusDto {
        public String username;
        public boolean isOnline;
        
        public UserStatusDto(String username, boolean isOnline) {
            this.username = username;
            this.isOnline = isOnline;
        }
    }

    public static class OnlineCountDto {
        public long count;
        
        public OnlineCountDto(long count) {
            this.count = count;
        }
    }
}