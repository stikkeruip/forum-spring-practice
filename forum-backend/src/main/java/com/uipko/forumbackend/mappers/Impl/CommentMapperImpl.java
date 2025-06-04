package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.CommentReaction;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.CommentMapper;
import com.uipko.forumbackend.repositories.CommentReactionRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CommentMapperImpl implements CommentMapper {
    private final CommentReactionRepository commentReactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public CommentMapperImpl(CommentReactionRepository commentReactionRepository,
                           CurrentUserProvider currentUserProvider) {
        this.commentReactionRepository = commentReactionRepository;
        this.currentUserProvider = currentUserProvider;
    }
    @Override
    public Comment createDtoToComment(CommentCreateDto commentCreateDto) {
        return new Comment(
                null,
                null,
                null,
                commentCreateDto.content(),
                LocalDateTime.now(),
                null,
                null,
                0,
                0,
                new HashSet<>(),
                null,
                new HashSet<>()
        );
    }

    @Override
    public CommentCreateResponseDto commentToCreateDto(Comment comment) {
        return new CommentCreateResponseDto(comment.getId(), comment.getContent());
    }

    @Override
    public Comment updateDtoToComment(CommentUpdateDto commentUpdateDto, Comment comment) {
        comment.setContent(commentUpdateDto.content());
        comment.setUpdatedDate(LocalDateTime.now());
        return comment;
    }

    @Override
    public CommentUpdateResponseDto commentToUpdateDto(Comment comment) {
        return new CommentUpdateResponseDto(comment.getContent());
    }

    @Override
    public CommentResponseDto commentToResponseDto(Comment comment) {
        List<CommentResponseDto> replies = comment.getReplies()
                .stream()
                .filter(reply -> reply.getDeletedDate() == null) // Only non-deleted replies
                .map(this::commentToResponseDto)
                .collect(Collectors.toList());
        
        // Get current user's reaction if authenticated
        String userReaction = null;
        try {
            User currentUser = currentUserProvider.getAuthenticatedUser();
            if (currentUser != null && !"anonymousUser".equals(currentUser.getName())) {
                Optional<CommentReaction> reaction = commentReactionRepository.findByCommentAndUser(comment, currentUser);
                userReaction = reaction.map(CommentReaction::getReactionType).orElse(null);
            }
        } catch (Exception e) {
            // User is not authenticated, userReaction stays null
        }
        
        return new CommentResponseDto(
                comment.getId(),
                comment.getUser().getName(),
                comment.getContent(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                (long) replies.size(), // replyCount based on actual replies
                comment.getLikes() != null ? comment.getLikes().longValue() : 0L,
                comment.getDislikes() != null ? comment.getDislikes().longValue() : 0L,
                userReaction,
                comment.getCreatedDate(),
                comment.getUpdatedDate(),
                replies
        );
    }
}
