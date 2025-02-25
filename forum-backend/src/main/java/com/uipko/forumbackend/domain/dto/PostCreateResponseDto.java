package com.uipko.forumbackend.domain.dto;

public record PostCreateResponseDto(
        Long id,
        String title,
        String content
) {
}
