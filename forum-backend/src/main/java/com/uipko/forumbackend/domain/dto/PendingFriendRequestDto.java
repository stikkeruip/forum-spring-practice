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
public class PendingFriendRequestDto {
    private Long friendshipId;
    private String requesterUsername;
    private LocalDateTime requestDate;
    private boolean isOnline;
}