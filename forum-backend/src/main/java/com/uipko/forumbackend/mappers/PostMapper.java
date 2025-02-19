package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.PostCreateResponseDto;
import com.uipko.forumbackend.domain.dto.PostUpdateDto;
import com.uipko.forumbackend.domain.dto.PostUpdateResponseDto;
import com.uipko.forumbackend.domain.entities.Post;

public interface PostMapper {
    Post createDtoToPost(PostCreateDto postCreateDto);

    PostCreateResponseDto postToCreateDto(Post post);

    Post updateDtoToPost(PostUpdateDto postUpdateDto, Post post);

    PostUpdateResponseDto postToUpdateDto(Post post);
}
