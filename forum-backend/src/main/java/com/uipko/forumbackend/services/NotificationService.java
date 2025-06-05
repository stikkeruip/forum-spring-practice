package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.domain.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    
    void createLikeNotification(User actor, User recipient, Post post);
    
    void createLikeNotification(User actor, User recipient, Comment comment);
    
    void createCommentNotification(User actor, User recipient, Post post, Comment comment);
    
    void createReplyNotification(User actor, User recipient, Comment parentComment, Comment reply);
    
    void createPostDeletionNotification(User actor, User recipient, Post post);
    
    void createCommentDeletionNotification(User actor, User recipient, Comment comment);
    
    void createPostRestorationNotification(User actor, User recipient, Post post);
    
    void createFriendRequestNotification(User requester, User addressee);
    
    void createFriendRequestAcceptedNotification(User accepter, User requester);
    
    void createFriendRequestDeclinedNotification(User decliner, User requester);
    
    Page<NotificationResponseDto> getUserNotifications(String username, Pageable pageable);
    
    long getUnreadCount(String username);
    
    void markAsRead(String username, List<Long> notificationIds);
    
    void markAllAsRead(String username);
}