package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.PostReaction;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.PostReactionRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

@Component
public class PostMapperImpl implements PostMapper {
    private final CommentRepository commentRepository;
    private final PostReactionRepository postReactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public PostMapperImpl(CommentRepository commentRepository, 
                         PostReactionRepository postReactionRepository,
                         CurrentUserProvider currentUserProvider) {
        this.commentRepository = commentRepository;
        this.postReactionRepository = postReactionRepository;
        this.currentUserProvider = currentUserProvider;
    }
    @Override
    public Post createDtoToPost(PostCreateDto postCreateDto) {
        return new Post(
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                null,
                postCreateDto.title(),
                postCreateDto.content(),
                0,
                0,
                new HashSet<>()
        );
    }

    @Override
    public PostCreateResponseDto postToCreateDto(Post post) {
        return new PostCreateResponseDto(
                post.getId(),
                post.getTitle(),
                post.getContent()
        );
    }

    @Override
    public Post updateDtoToPost(PostUpdateDto postUpdateDto, Post post) {
        post.setContent(postUpdateDto.content());
        post.setUpdatedDate(LocalDateTime.now());
        return post;
    }

    @Override
    public PostUpdateResponseDto postToUpdateDto(Post post) {
        return new PostUpdateResponseDto(post.getContent());
    }

    @Override
    public PostResponseDto postToResponseDto(Post post) {
        Long commentCount = (long) commentRepository.getCommentsByPostIdAndDeletedDateIsNull(post.getId()).size();
        
        // Get current user's reaction if authenticated
        String userReaction = null;
        try {
            User currentUser = currentUserProvider.getAuthenticatedUser();
            if (currentUser != null && !"anonymousUser".equals(currentUser.getName())) {
                Optional<PostReaction> reaction = postReactionRepository.findByPostAndUser(post, currentUser);
                userReaction = reaction.map(PostReaction::getReactionType).orElse(null);
            }
        } catch (Exception e) {
            // User is not authenticated, userReaction stays null
        }
        
        return new PostResponseDto(
                post.getId(),
                post.getUser().getName(),
                post.getTitle(),
                post.getContent(),
                commentCount,
                post.getLikes() != null ? post.getLikes().longValue() : 0L,
                post.getDislikes() != null ? post.getDislikes().longValue() : 0L,
                userReaction,
                post.getCreatedDate(),
                post.getUpdatedDate(),
                post.getDeletedDate(),
                post.getDeletedBy() != null ? post.getDeletedBy().getName() : null
        );
    }
}
