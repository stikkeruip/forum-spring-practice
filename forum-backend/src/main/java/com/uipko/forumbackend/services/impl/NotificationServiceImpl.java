package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.controllers.WebSocketController;
import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.domain.entities.*;
import com.uipko.forumbackend.mappers.NotificationMapper;
import com.uipko.forumbackend.repositories.NotificationRepository;
import com.uipko.forumbackend.services.NotificationService;
import com.uipko.forumbackend.services.RedisMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final WebSocketController webSocketController;
    private final RedisMessagingService redisMessagingService;
    
    @Override
    @Transactional
    public void createLikeNotification(User actor, User recipient, Post post) {
        if (actor.getName().equals(recipient.getName())) {
            return; // Don't notify users about their own actions
        }
        
        String message = String.format("%s liked your post: \"%s\"", 
                actor.getName(), 
                truncateText(post.getTitle(), 50));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.POST_LIKED)
                .targetPost(post)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createLikeNotification(User actor, User recipient, Comment comment) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("%s liked your comment: \"%s\"", 
                actor.getName(), 
                truncateText(comment.getContent(), 50));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.COMMENT_LIKED)
                .targetComment(comment)
                .targetPost(comment.getPost())
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createCommentNotification(User actor, User recipient, Post post, Comment comment) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("%s commented on your post: \"%s\"", 
                actor.getName(), 
                truncateText(post.getTitle(), 50));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.POST_COMMENTED)
                .targetPost(post)
                .targetComment(comment)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createReplyNotification(User actor, User recipient, Comment parentComment, Comment reply) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("%s replied to your comment: \"%s\"", 
                actor.getName(), 
                truncateText(parentComment.getContent(), 50));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.COMMENT_REPLIED)
                .targetComment(reply)
                .targetPost(parentComment.getPost())
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createPostDeletionNotification(User actor, User recipient, Post post) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("Your post \"%s\" was deleted by %s", 
                truncateText(post.getTitle(), 50),
                actor.getName());
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.POST_DELETED_BY_MODERATOR)
                .targetPost(post)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createCommentDeletionNotification(User actor, User recipient, Comment comment) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("Your comment \"%s\" was deleted by %s", 
                truncateText(comment.getContent(), 50),
                actor.getName());
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.COMMENT_DELETED_BY_MODERATOR)
                .targetComment(comment)
                .targetPost(comment.getPost())
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createPostRestorationNotification(User actor, User recipient, Post post) {
        if (actor.getName().equals(recipient.getName())) {
            return;
        }
        
        String message = String.format("Your post \"%s\" was restored by %s", 
                truncateText(post.getTitle(), 50),
                actor.getName());
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(Notification.NotificationType.POST_RESTORED_BY_MODERATOR)
                .targetPost(post)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createFriendRequestNotification(User requester, User addressee) {
        String message = String.format("%s sent you a friend request", requester.getName());
        
        Notification notification = Notification.builder()
                .recipient(addressee)
                .actor(requester)
                .type(Notification.NotificationType.FRIEND_REQUEST_SENT)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createFriendRequestAcceptedNotification(User accepter, User requester) {
        String message = String.format("%s accepted your friend request", accepter.getName());
        
        Notification notification = Notification.builder()
                .recipient(requester)
                .actor(accepter)
                .type(Notification.NotificationType.FRIEND_REQUEST_ACCEPTED)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional
    public void createFriendRequestDeclinedNotification(User decliner, User requester) {
        String message = String.format("%s declined your friend request", decliner.getName());
        
        Notification notification = Notification.builder()
                .recipient(requester)
                .actor(decliner)
                .type(Notification.NotificationType.FRIEND_REQUEST_DECLINED)
                .message(message)
                .read(false)
                .build();
        
        notification = notificationRepository.save(notification);
        sendRealTimeNotification(notification);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getUserNotifications(String username, Pageable pageable) {
        return notificationRepository.findByRecipientNameOrderByCreatedDateDesc(username, pageable)
                .map(notificationMapper::toResponseDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countUnreadByRecipientUsername(username);
    }
    
    @Override
    @Transactional
    public void markAsRead(String username, List<Long> notificationIds) {
        if (notificationIds != null && !notificationIds.isEmpty()) {
            notificationRepository.markAsReadByIdsAndUsername(notificationIds, username);
            webSocketController.sendUnreadCountUpdate(username, getUnreadCount(username));
        }
    }
    
    @Override
    @Transactional
    public void markAllAsRead(String username) {
        notificationRepository.markAllAsReadByUsername(username);
        webSocketController.sendUnreadCountUpdate(username, 0L);
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    private void sendRealTimeNotification(Notification notification) {
        NotificationResponseDto dto = notificationMapper.toResponseDto(notification);
        
        // Send via WebSocket (existing)
        webSocketController.sendNotification(notification.getRecipient().getName(), dto);
        long unreadCount = notificationRepository.countUnreadByRecipientUsername(
                notification.getRecipient().getName());
        webSocketController.sendUnreadCountUpdate(notification.getRecipient().getName(), unreadCount);
        
        // Publish to Redis for distributed real-time updates
        redisMessagingService.publishNotification(notification);
        
        log.debug("Sent real-time notification to user: {} via WebSocket and Redis", 
                notification.getRecipient().getName());
    }
}