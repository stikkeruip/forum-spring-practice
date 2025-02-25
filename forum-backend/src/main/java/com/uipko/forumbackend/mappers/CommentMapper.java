package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.Comment;

public interface CommentMapper {
    Comment createDtoToComment(CommentCreateDto commentCreateDto);

    CommentCreateResponseDto commentToCreateDto(Comment comment);

    Comment updateDtoToComment(CommentUpdateDto commentUpdateDto, Comment comment);

    CommentUpdateResponseDto commentToUpdateDto(Comment comment);

    CommentResponseDto commentToResponseDto(Comment comment);
}
