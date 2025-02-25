package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Post;

public interface PostMapper {
    Post createDtoToPost(PostCreateDto postCreateDto);

    PostCreateResponseDto postToCreateDto(Post post);

    Post updateDtoToPost(PostUpdateDto postUpdateDto, Post post);

    PostUpdateResponseDto postToUpdateDto(Post post);

    PostResponseDto postToResponseDto(Post post);
}
