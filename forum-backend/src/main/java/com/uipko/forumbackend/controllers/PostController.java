package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.PostCreateResponseDto;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.services.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;

    @PostMapping(path = "/create-post")
    public PostCreateResponseDto createPost(@RequestBody PostCreateDto postCreateDto) {

        Post post = postService.createPost(postMapper.createDtoToPost(postCreateDto));

        return postMapper.postToCreateDto(post);
    }
}
