package com.uipko.forumbackend.domain.dto;

import com.uipko.forumbackend.domain.entities.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private Long id;
    private String actorUsername;
    private Notification.NotificationType type;
    private Long targetPostId;
    private Long targetCommentId;
    private String message;
    private boolean read;
    private LocalDateTime createdDate;
}