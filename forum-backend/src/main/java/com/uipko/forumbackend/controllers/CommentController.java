package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.CommentCreateDto;
import com.uipko.forumbackend.domain.dto.CommentCreateResponseDto;
import com.uipko.forumbackend.domain.dto.CommentResponseDto;
import com.uipko.forumbackend.domain.dto.ReactionDto;
import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.mappers.CommentMapper;
import com.uipko.forumbackend.services.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentMapper commentMapper;

    public CommentController(CommentService commentService, CommentMapper commentMapper) {
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    @PostMapping
    public ResponseEntity<CommentCreateResponseDto> createComment(
            @RequestBody CommentCreateDto commentCreateDto,
            @PathVariable Long postId) {
        Comment comment = commentService.createComment(commentMapper.createDtoToComment(commentCreateDto), postId);
        return ResponseEntity.ok(commentMapper.commentToCreateDto(comment));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getCommentsByPost(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPost(postId);
        List<CommentResponseDto> commentDtos = comments.stream()
                .map(commentMapper::commentToResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDtos);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> getComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        Comment comment = commentService.getComment(commentId);
        return ResponseEntity.ok(commentMapper.commentToResponseDto(comment));
    }

    @PostMapping("/{commentId}/reactions")
    public ResponseEntity<CommentResponseDto> reactToComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody ReactionDto reactionDto) {
        Comment comment = commentService.reactToComment(commentId, reactionDto.getReactionType());
        return ResponseEntity.ok(commentMapper.commentToResponseDto(comment));
    }
}
