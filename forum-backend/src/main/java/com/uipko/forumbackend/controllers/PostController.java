package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.PostCreateResponseDto;
import com.uipko.forumbackend.domain.dto.PostResponseDto;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.services.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/posts")
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;

    public PostController(PostService postService, PostMapper postMapper) {
        this.postService = postService;
        this.postMapper = postMapper;
    }

    @PostMapping
    public ResponseEntity<PostCreateResponseDto> createPost(@RequestBody PostCreateDto postCreateDto) {

        Post post = postService.createPost(postMapper.createDtoToPost(postCreateDto));

        return ResponseEntity.ok(postMapper.postToCreateDto(post));
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(postMapper.postToResponseDto(post));
    }
}
