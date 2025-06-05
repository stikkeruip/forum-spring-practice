package com.uipko.forumbackend.domain.dto.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for caching notifications without lazy loading issues
 * Contains only the essential data needed for caching and serialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheableNotificationDto {
    
    private Long id;
    private String recipientName;
    private String actorName;
    private String type; // NotificationType as string
    private String message;
    private boolean read;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;
    
    // Target references as simple data instead of full entities
    private Long targetPostId;
    private String targetPostTitle;
    private Long targetCommentId;
    private String targetCommentContent;
}