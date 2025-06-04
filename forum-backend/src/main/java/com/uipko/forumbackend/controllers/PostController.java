package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.PostCreateDto;
import com.uipko.forumbackend.domain.dto.PostCreateResponseDto;
import com.uipko.forumbackend.domain.dto.PostResponseDto;
import com.uipko.forumbackend.domain.dto.ReactionDto;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.services.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;
    private final PostMapper postMapper;

    public PostController(PostService postService, PostMapper postMapper) {
        this.postService = postService;
        this.postMapper = postMapper;
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        logger.info("Deleting post with ID: {}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/restore")
    public ResponseEntity<Void> restorePost(@PathVariable Long id) {
        logger.info("Restoring post with ID: {}", id);
        postService.restorePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<PostCreateResponseDto> createPost(@RequestBody PostCreateDto postCreateDto) {
        logger.info("Creating new post: {}", postCreateDto.title());
        Post post = postService.createPost(postMapper.createDtoToPost(postCreateDto));
        logger.info("Post created with ID: {}", post.getId());
        return ResponseEntity.ok(postMapper.postToCreateDto(post));
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        Post post = postService.getPost(id);
        return ResponseEntity.ok(postMapper.postToResponseDto(post));
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        List<PostResponseDto> postResponseDtos = posts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(postResponseDtos);
    }

    @GetMapping(path = "/deleted")
    public ResponseEntity<List<PostResponseDto>> getDeletedPosts() {
        List<Post> deletedPosts = postService.getDeletedPosts();
        List<PostResponseDto> postResponseDtos = deletedPosts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(postResponseDtos);
    }

    @GetMapping(path = "/deleted/{id}")
    public ResponseEntity<PostResponseDto> getDeletedPostById(@PathVariable Long id) {
        Post deletedPost = postService.getDeletedPostById(id);
        return ResponseEntity.ok(postMapper.postToResponseDto(deletedPost));
    }

    @PostMapping(path = "/{id}/reactions")
    public ResponseEntity<PostResponseDto> reactToPost(
            @PathVariable Long id,
            @RequestBody ReactionDto reactionDto) {
        logger.info("Reaction {} on post ID: {}", reactionDto.getReactionType(), id);
        Post post = postService.reactToPost(id, reactionDto.getReactionType());
        return ResponseEntity.ok(postMapper.postToResponseDto(post));
    }
}
