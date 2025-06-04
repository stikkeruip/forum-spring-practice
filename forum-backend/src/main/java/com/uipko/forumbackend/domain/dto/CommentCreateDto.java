package com.uipko.forumbackend.domain.dto;

public record CommentCreateDto(String content, Long parentCommentId) {
}
