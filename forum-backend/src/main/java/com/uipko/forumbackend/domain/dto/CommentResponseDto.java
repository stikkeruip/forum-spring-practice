package com.uipko.forumbackend.domain.dto;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Long id,
        String owner,
        String content,
        Long replyCount,
        Long likeCount,
        Long dislikeCount,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {
}
