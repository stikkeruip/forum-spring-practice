package com.uipko.forumbackend.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEvent {
    private Long postId;
    private String action; // CREATED, UPDATED, DELETED, RESTORED
    private String authorUsername;
    private String title;
    private LocalDateTime timestamp;
    
    public static PostEvent created(Long postId, String authorUsername, String title) {
        return PostEvent.builder()
                .postId(postId)
                .action("CREATED")
                .authorUsername(authorUsername)
                .title(title)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static PostEvent deleted(Long postId, String authorUsername) {
        return PostEvent.builder()
                .postId(postId)
                .action("DELETED")
                .authorUsername(authorUsername)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static PostEvent restored(Long postId, String authorUsername) {
        return PostEvent.builder()
                .postId(postId)
                .action("RESTORED")
                .authorUsername(authorUsername)
                .timestamp(LocalDateTime.now())
                .build();
    }
}