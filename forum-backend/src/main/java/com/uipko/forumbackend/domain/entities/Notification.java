package com.uipko.forumbackend.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_username", referencedColumnName = "name", nullable = false)
    private User recipient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_username", referencedColumnName = "name", nullable = false)
    private User actor;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private NotificationType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_post_id")
    private Post targetPost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_comment_id")
    private Comment targetComment;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;
    
    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    public enum NotificationType {
        POST_LIKED,
        COMMENT_LIKED,
        POST_COMMENTED,
        COMMENT_REPLIED,
        POST_DELETED_BY_MODERATOR,
        COMMENT_DELETED_BY_MODERATOR,
        POST_RESTORED_BY_MODERATOR,
        FRIEND_REQUEST_SENT,
        FRIEND_REQUEST_ACCEPTED,
        FRIEND_REQUEST_DECLINED
    }
}