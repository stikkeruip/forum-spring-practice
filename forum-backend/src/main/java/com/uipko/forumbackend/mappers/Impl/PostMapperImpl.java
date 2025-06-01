package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.repositories.CommentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;

@Component
public class PostMapperImpl implements PostMapper {
    private final CommentRepository commentRepository;

    public PostMapperImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    @Override
    public Post createDtoToPost(PostCreateDto postCreateDto) {
        return new Post(
                null,
                null,
                LocalDateTime.now(),
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
        return new PostResponseDto(
                post.getId(),
                post.getUser().getName(),
                post.getTitle(),
                post.getContent(),
                commentCount,
                post.getLikes() != null ? post.getLikes().longValue() : 0L,
                post.getDislikes() != null ? post.getDislikes().longValue() : 0L,
                post.getCreatedDate(),
                post.getUpdatedDate(),
                post.getDeletedDate()
        );
    }
}
