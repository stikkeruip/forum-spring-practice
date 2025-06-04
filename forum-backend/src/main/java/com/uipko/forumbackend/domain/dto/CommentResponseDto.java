package com.uipko.forumbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponseDto(
        Long id,
        String owner,
        String content,
        Long parentCommentId,
        Long replyCount,
        Long likeCount,
        Long dislikeCount,
        String userReaction, // "LIKE", "DISLIKE", or null
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        List<CommentResponseDto> replies
) {
}
