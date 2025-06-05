package com.uipko.forumbackend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FriendshipStatusDto {
    private String status; // "NONE", "PENDING_SENT", "PENDING_RECEIVED", "ACCEPTED", "DECLINED", "BLOCKED"
    private Long friendshipId;
    private boolean canSendRequest;
}