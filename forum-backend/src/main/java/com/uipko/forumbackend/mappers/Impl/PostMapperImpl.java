package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.mappers.PostMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PostMapperImpl implements PostMapper {
    @Override
    public Post createDtoToPost(PostCreateDto postCreateDto) {
        return new Post(
                null,
                null,
                LocalDateTime.now(),
                null,
                null,
                postCreateDto.title(),
                postCreateDto.content()
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
        return new PostResponseDto(
                post.getId(),
                post.getUser().getName(),
                post.getTitle(),
                post.getContent(),
                null,
                null,
                null,
                post.getCreatedDate(),
                post.getUpdatedDate()
        );
    }
}
