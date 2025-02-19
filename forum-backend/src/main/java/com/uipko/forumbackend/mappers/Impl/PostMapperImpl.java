package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.PostCreateResponseDto;
import com.uipko.forumbackend.domain.dto.PostUpdateDto;
import com.uipko.forumbackend.domain.dto.PostUpdateResponseDto;
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
}
