package com.uipko.forumbackend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfileResponseDto {
    private String username;
    private LocalDateTime createdDate;
    private List<PostResponseDto> posts;
}