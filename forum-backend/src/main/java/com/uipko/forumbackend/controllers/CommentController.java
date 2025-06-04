package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.CommentCreateDto;
import com.uipko.forumbackend.domain.dto.CommentCreateResponseDto;
import com.uipko.forumbackend.domain.dto.CommentResponseDto;
import com.uipko.forumbackend.domain.dto.ReactionDto;
import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.mappers.CommentMapper;
import com.uipko.forumbackend.services.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

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
        logger.info("Creating comment on post ID: {}", postId);
        
        Comment comment;
        if (commentCreateDto.parentCommentId() != null) {
            // Creating a reply
            logger.info("Creating reply to comment ID: {}", commentCreateDto.parentCommentId());
            comment = commentService.createReply(commentMapper.createDtoToComment(commentCreateDto), postId, commentCreateDto.parentCommentId());
        } else {
            // Creating a top-level comment
            comment = commentService.createComment(commentMapper.createDtoToComment(commentCreateDto), postId);
        }
        
        logger.info("Comment created with ID: {}", comment.getId());
        return ResponseEntity.ok(commentMapper.commentToCreateDto(comment));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getCommentsByPost(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPost(postId);
        // Only return top-level comments (replies are nested within them)
        List<CommentResponseDto> commentDtos = comments.stream()
                .filter(comment -> comment.getParentComment() == null)
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
        logger.info("Reaction {} on comment ID: {}", reactionDto.getReactionType(), commentId);
        Comment comment = commentService.reactToComment(commentId, reactionDto.getReactionType());
        return ResponseEntity.ok(commentMapper.commentToResponseDto(comment));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        logger.info("Deleting comment ID: {} from post ID: {}", commentId, postId);
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
