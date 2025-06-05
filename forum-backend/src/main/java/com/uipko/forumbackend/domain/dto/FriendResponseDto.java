package com.uipko.forumbackend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FriendResponseDto {
    
    @NotNull(message = "Friendship ID is required")
    private Long friendshipId;
    
    @NotBlank(message = "Response is required")
    private String response; // "ACCEPT" or "DECLINE"
}