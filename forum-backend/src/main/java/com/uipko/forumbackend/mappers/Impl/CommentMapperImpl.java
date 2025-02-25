package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.mappers.CommentMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommentMapperImpl implements CommentMapper {
    @Override
    public Comment createDtoToComment(CommentCreateDto commentCreateDto) {
        return new Comment(
                null,
                null,
                null,
                commentCreateDto.content(),
                LocalDateTime.now(),
                null,
                null
        );
    }

    @Override
    public CommentCreateResponseDto commentToCreateDto(Comment comment) {
        return new CommentCreateResponseDto(comment.getId(), comment.getContent());
    }

    @Override
    public Comment updateDtoToComment(CommentUpdateDto commentUpdateDto, Comment comment) {
        comment.setContent(commentUpdateDto.content());
        comment.setUpdatedDate(LocalDateTime.now());
        return comment;
    }

    @Override
    public CommentUpdateResponseDto commentToUpdateDto(Comment comment) {
        return new CommentUpdateResponseDto(comment.getContent());
    }

    @Override
    public CommentResponseDto commentToResponseDto(Comment comment) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getUser().getName(),
                comment.getContent(),
                null,
                null,
                null,
                comment.getCreatedDate(),
                comment.getUpdatedDate()
        );
    }
}
