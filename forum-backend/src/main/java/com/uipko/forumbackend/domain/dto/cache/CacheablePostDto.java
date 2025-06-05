package com.uipko.forumbackend.domain.dto.cache;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for caching posts without lazy loading issues
 * Contains only the essential data needed for caching and serialization
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheablePostDto {
    
    private Long id;
    private String userName;
    private String userRole;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedDate;
    
    private String deletedByUserName;
    private String title;
    private String content;
    private Integer likes;
    private Integer dislikes;
    
    // Reaction summary instead of full reaction entities
    private Set<CacheableReactionDto> reactions;
    
    // Comment count instead of full comment entities (to avoid deep nesting)
    private Integer commentCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheableReactionDto {
        private Long id;
        private String userName;
        private String reactionType; // LIKE, DISLIKE
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdDate;
    }
}