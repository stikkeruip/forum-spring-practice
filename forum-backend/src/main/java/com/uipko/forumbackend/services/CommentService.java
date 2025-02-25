package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.Comment;

import java.util.List;

public interface CommentService {
    Comment createComment(Comment comment, Long postId);

    Comment getComment(Long id);

    Comment updateComment(Long id, Comment comment);

    void deleteComment(Long id); //TODO make soft delete

    List<Comment> getCommentsByUser(String userName);

    List<Comment> getCommentsByPost(Long postId);
}
