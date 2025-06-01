package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.CommentReaction;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.CommentContentEmptyException;
import com.uipko.forumbackend.exceptions.CommentDeleteUnauthorizedException;
import com.uipko.forumbackend.exceptions.CommentNotFoundException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.repositories.CommentReactionRepository;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.CommentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    private final CurrentUserProvider currentUserProvider;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;

    public CommentServiceImpl(CurrentUserProvider currentUserProvider, PostRepository postRepository, 
                             CommentRepository commentRepository, CommentReactionRepository commentReactionRepository) {
        this.currentUserProvider = currentUserProvider;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.commentReactionRepository = commentReactionRepository;
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @commentOwnershipEvaluator.isOwner(authentication, #id)")
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @commentOwnershipEvaluator.isOwner(authentication, #id)")
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new CommentNotFoundException(id));

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

    @Transactional
    @Override
    public Comment reactToComment(Long commentId, String reactionType) {
        Comment comment = commentRepository.findByIdAndDeletedDateIsNull(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        User user = currentUserProvider.getAuthenticatedUser();

        // Check if user already reacted to this comment
        Optional<CommentReaction> existingReaction = commentReactionRepository.findByCommentAndUser(comment, user);

        if (existingReaction.isPresent()) {
            CommentReaction reaction = existingReaction.get();
            String oldReactionType = reaction.getReactionType();

            // If same reaction type, remove the reaction (toggle)
            if (oldReactionType.equals(reactionType)) {
                commentReactionRepository.delete(reaction);

                // Update comment counts
                if ("LIKE".equals(reactionType)) {
                    comment.setLikes(comment.getLikes() - 1);
                } else if ("DISLIKE".equals(reactionType)) {
                    comment.setDislikes(comment.getDislikes() - 1);
                }
            } else {
                // Change reaction type
                reaction.setReactionType(reactionType);
                reaction.setCreatedDate(LocalDateTime.now());
                commentReactionRepository.save(reaction);

                // Update comment counts
                if ("LIKE".equals(reactionType)) {
                    comment.setLikes(comment.getLikes() + 1);
                    comment.setDislikes(comment.getDislikes() - 1);
                } else if ("DISLIKE".equals(reactionType)) {
                    comment.setDislikes(comment.getDislikes() + 1);
                    comment.setLikes(comment.getLikes() - 1);
                }
            }
        } else {
            // Create new reaction
            CommentReaction reaction = new CommentReaction();
            reaction.setComment(comment);
            reaction.setUser(user);
            reaction.setReactionType(reactionType);
            reaction.setCreatedDate(LocalDateTime.now());
            commentReactionRepository.save(reaction);

            // Update comment counts
            if ("LIKE".equals(reactionType)) {
                comment.setLikes(comment.getLikes() + 1);
            } else if ("DISLIKE".equals(reactionType)) {
                comment.setDislikes(comment.getDislikes() + 1);
            }
        }

        return commentRepository.save(comment);
    }
}
