package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.cache.CacheableNotificationDto;
import com.uipko.forumbackend.domain.dto.cache.CacheablePostDto;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.PostReaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for converting entities to cache-safe DTOs
 * Prevents LazyInitializationException by avoiding lazy relationships
 */
@Component
public class CacheMapper {
    
    /**
     * Convert Post entity to cacheable DTO
     * SAFE: Avoids accessing lazy collections that may cause LazyInitializationException
     * Uses pre-calculated values and eager-loaded data only
     */
    public CacheablePostDto toDto(Post post) {
        if (post == null) {
            return null;
        }
        
        // Only access collections if they are already loaded (Hibernate.isInitialized check)
        Set<CacheablePostDto.CacheableReactionDto> reactionDtos = Set.of();
        if (post.getReactions() != null && org.hibernate.Hibernate.isInitialized(post.getReactions())) {
            reactionDtos = post.getReactions().stream()
                .map(this::toReactionDto)
                .collect(Collectors.toSet());
        }
        
        // Use pre-calculated comment count instead of accessing lazy collection
        // This avoids LazyInitializationException from post.getComments().size()
        Integer commentCount = 0; // Default value
        // The comment count should be calculated during query or passed separately
        // We'll rely on the repository to provide this information
        
        return CacheablePostDto.builder()
            .id(post.getId())
            .userName(post.getUser() != null ? post.getUser().getName() : null)
            .userRole(post.getUser() != null ? post.getUser().getRole() : null)
            .createdDate(post.getCreatedDate())
            .updatedDate(post.getUpdatedDate())
            .deletedDate(post.getDeletedDate())
            .deletedByUserName(post.getDeletedBy() != null ? post.getDeletedBy().getName() : null)
            .title(post.getTitle())
            .content(post.getContent())
            .likes(post.getLikes())
            .dislikes(post.getDislikes())
            .reactions(reactionDtos)
            .commentCount(commentCount) // Safe: no lazy loading
            .build();
    }
    
    /**
     * Safe version that accepts pre-calculated comment count
     * Use this method when you have the comment count available to avoid lazy loading
     */
    public CacheablePostDto toDto(Post post, Integer commentCount) {
        if (post == null) {
            return null;
        }
        
        Set<CacheablePostDto.CacheableReactionDto> reactionDtos = Set.of();
        if (post.getReactions() != null && org.hibernate.Hibernate.isInitialized(post.getReactions())) {
            reactionDtos = post.getReactions().stream()
                .map(this::toReactionDto)
                .collect(Collectors.toSet());
        }
        
        return CacheablePostDto.builder()
            .id(post.getId())
            .userName(post.getUser() != null ? post.getUser().getName() : null)
            .userRole(post.getUser() != null ? post.getUser().getRole() : null)
            .createdDate(post.getCreatedDate())
            .updatedDate(post.getUpdatedDate())
            .deletedDate(post.getDeletedDate())
            .deletedByUserName(post.getDeletedBy() != null ? post.getDeletedBy().getName() : null)
            .title(post.getTitle())
            .content(post.getContent())
            .likes(post.getLikes())
            .dislikes(post.getDislikes())
            .reactions(reactionDtos)
            .commentCount(commentCount != null ? commentCount : 0)
            .build();
    }
    
    /**
     * Convert PostReaction entity to cacheable DTO
     */
    private CacheablePostDto.CacheableReactionDto toReactionDto(PostReaction reaction) {
        if (reaction == null) {
            return null;
        }
        
        return CacheablePostDto.CacheableReactionDto.builder()
            .id(reaction.getId())
            .userName(reaction.getUser() != null ? reaction.getUser().getName() : null)
            .reactionType(reaction.getReactionType())
            .createdDate(reaction.getCreatedDate())
            .build();
    }
    
    /**
     * Convert Notification entity to cacheable DTO
     */
    public CacheableNotificationDto toDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        
        return CacheableNotificationDto.builder()
            .id(notification.getId())
            .recipientName(notification.getRecipient() != null ? notification.getRecipient().getName() : null)
            .actorName(notification.getActor() != null ? notification.getActor().getName() : null)
            .type(notification.getType() != null ? notification.getType().name() : null)
            .message(notification.getMessage())
            .read(notification.isRead())
            .createdDate(notification.getCreatedDate())
            .targetPostId(notification.getTargetPost() != null ? notification.getTargetPost().getId() : null)
            .targetPostTitle(notification.getTargetPost() != null ? notification.getTargetPost().getTitle() : null)
            .targetCommentId(notification.getTargetComment() != null ? notification.getTargetComment().getId() : null)
            .targetCommentContent(notification.getTargetComment() != null ? 
                (notification.getTargetComment().getContent().length() > 100 ? 
                    notification.getTargetComment().getContent().substring(0, 100) + "..." : 
                    notification.getTargetComment().getContent()) : null)
            .build();
    }
    
    /**
     * Batch convert posts to DTOs
     */
    public List<CacheablePostDto> toPostDtos(List<Post> posts) {
        return posts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Batch convert notifications to DTOs
     */
    public List<CacheableNotificationDto> toNotificationDtos(List<Notification> notifications) {
        return notifications.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}