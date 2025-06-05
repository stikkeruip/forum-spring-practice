package com.uipko.forumbackend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FriendDto {
    private String username;
    private LocalDateTime createdDate;
    private boolean isOnline;
    private LocalDateTime lastSeen;
    private LocalDateTime friendshipDate;
}