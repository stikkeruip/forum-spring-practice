package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.CommentContentEmptyException;
import com.uipko.forumbackend.exceptions.CommentDeleteUnauthorizedException;
import com.uipko.forumbackend.exceptions.CommentNotFoundException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.CommentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private final CurrentUserProvider currentUserProvider;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public CommentServiceImpl(CurrentUserProvider currentUserProvider, PostRepository postRepository, CommentRepository commentRepository) {
        this.currentUserProvider = currentUserProvider;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    @Override
    public Comment createComment(Comment comment, Long postId) {
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new CommentContentEmptyException();
        }

        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));
        User user = currentUserProvider.getAuthenticatedUser();

        comment.setPost(post);
        comment.setUser(user);
        return commentRepository.save(comment);
    }

    @Override
    public Comment getComment(Long id) {
        return commentRepository.findByIdAndDeletedDateIsNull(id).orElseThrow(() -> new CommentNotFoundException(id));
    }

    @Override
    public Comment updateComment(Long id, Comment newComment) {
        if (newComment.getContent() == null || newComment.getContent().isBlank()) {
            throw new CommentContentEmptyException();
        }

        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));

        comment.setContent(newComment.getContent());
        comment.setUpdatedDate(newComment.getUpdatedDate());
        return commentRepository.save(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));
        User user = currentUserProvider.getAuthenticatedUser();

        if (!user.equals(comment.getUser())) {
            throw new CommentDeleteUnauthorizedException(user.getName());
        }

        comment.setDeletedDate(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentsByUser(String userName) {
        return commentRepository.getCommentsByUserNameAndDeletedDateIsNull(userName);
    }

    @Override
    public List<Comment> getCommentsByPost(Long postId) {
        return commentRepository.getCommentsByPostIdAndDeletedDateIsNull(postId);
    }
}
