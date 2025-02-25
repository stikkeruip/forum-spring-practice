package com.uipko.forumbackend.domain.dto;

import java.time.LocalDateTime;

public record PostResponseDto(
        Long postId,
        String owner,
        String title,
        String content,
        Long commentCount,
        Long likeCount,
        Long dislikeCount,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
