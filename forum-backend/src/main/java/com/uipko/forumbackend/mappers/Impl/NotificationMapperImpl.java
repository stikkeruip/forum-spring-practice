package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.domain.entities.Notification;
import com.uipko.forumbackend.mappers.NotificationMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapperImpl implements NotificationMapper {
    
    @Override
    public NotificationResponseDto toResponseDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .actorUsername(notification.getActor() != null ? notification.getActor().getName() : null)
                .type(notification.getType())
                .targetPostId(notification.getTargetPost() != null ? notification.getTargetPost().getId() : null)
                .targetCommentId(notification.getTargetComment() != null ? notification.getTargetComment().getId() : null)
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdDate(notification.getCreatedDate())
                .build();
    }
}