package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.NotificationResponseDto;
import com.uipko.forumbackend.domain.entities.Notification;

public interface NotificationMapper {
    NotificationResponseDto toResponseDto(Notification notification);
}