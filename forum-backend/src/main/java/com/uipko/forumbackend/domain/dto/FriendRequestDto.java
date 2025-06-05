package com.uipko.forumbackend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FriendRequestDto {
    
    @NotBlank(message = "Friend username is required")
    private String friendUsername;
}