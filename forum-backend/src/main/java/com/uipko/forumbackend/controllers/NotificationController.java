package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.NotificationMarkReadDto;
import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String username = principal.getName();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDto> notifications = notificationService.getUserNotifications(username, pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
        String username = principal.getName();
        long count = notificationService.getUnreadCount(username);
        
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @PostMapping("/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            Principal principal,
            @RequestBody NotificationMarkReadDto markReadDto) {
        
        String username = principal.getName();
        notificationService.markAsRead(username, markReadDto.getNotificationIds());
        
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    @PostMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        String username = principal.getName();
        notificationService.markAllAsRead(username);
        
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}