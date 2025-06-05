package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.services.OnlineActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final OnlineActivityService onlineActivityService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();
        
        if (user != null && sessionId != null) {
            String username = user.getName();
            onlineActivityService.setUserOnline(username, sessionId);
            log.info("WebSocket connection established for user: {} with session: {}", username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = event.getSessionId();
        
        if (user != null && sessionId != null) {
            String username = user.getName();
            onlineActivityService.setUserOffline(username, sessionId);
            log.info("WebSocket connection closed for user: {} with session: {}", username, sessionId);
        }
    }

    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat-response")
    public String handleHeartbeat(SimpMessageHeaderAccessor headerAccessor) {
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String username = user.getName();
            onlineActivityService.updateLastSeen(username);
            log.debug("Heartbeat received from user: {}", username);
            return "pong";
        }
        
        return "unauthorized";
    }

    @MessageMapping("/online-users")
    @SendTo("/topic/online-users")
    public Object getOnlineUsers() {
        return onlineActivityService.getOnlineUsers();
    }

    @MessageMapping("/online-count")
    @SendTo("/topic/online-count")
    public Object getOnlineCount() {
        return new OnlineCountResponse(onlineActivityService.getOnlineUserCount());
    }

    public static class OnlineCountResponse {
        public long count;
        
        public OnlineCountResponse(long count) {
            this.count = count;
        }
    }
    
    public void sendNotification(String username, NotificationResponseDto notification) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }
    
    public void sendUnreadCountUpdate(String username, long unreadCount) {
        messagingTemplate.convertAndSendToUser(username, "/queue/unread-count", 
                new UnreadCountResponse(unreadCount));
    }
    
    public static class UnreadCountResponse {
        public long count;
        
        public UnreadCountResponse(long count) {
            this.count = count;
        }
    }
    
    public void sendFriendshipUpdate(String username, String type, String friendUsername) {
        FriendshipUpdateMessage message = new FriendshipUpdateMessage(type, friendUsername);
        messagingTemplate.convertAndSendToUser(username, "/queue/friendship-updates", message);
    }
    
    public void sendFriendsListUpdate(String username) {
        messagingTemplate.convertAndSendToUser(username, "/queue/friends-list-update", "refresh");
    }
    
    public static class FriendshipUpdateMessage {
        public String type; // "request_sent", "request_received", "accepted", "declined", "removed"
        public String friendUsername;
        
        public FriendshipUpdateMessage(String type, String friendUsername) {
            this.type = type;
            this.friendUsername = friendUsername;
        }
    }
}