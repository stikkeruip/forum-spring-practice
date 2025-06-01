package com.uipko.forumbackend.security;

import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Custom permission evaluator to check if a user is the owner of a comment.
 * This is used in method security expressions like @PreAuthorize("hasRole('ADMIN') or @commentOwnershipEvaluator.isOwner(authentication, #id)")
 */
@Component
public class CommentOwnershipEvaluator {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentOwnershipEvaluator(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if the authenticated user is the owner of the comment with the given id.
     *
     * @param authentication the authentication object containing the user details
     * @param commentId the id of the comment to check ownership for
     * @return true if the user is the owner of the comment, false otherwise
     */
    public boolean isOwner(Authentication authentication, Long commentId) {
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findUserByName(username);
        Optional<Comment> commentOptional = commentRepository.findById(commentId);
        
        if (userOptional.isEmpty() || commentOptional.isEmpty()) {
            return false;
        }
        
        User user = userOptional.get();
        Comment comment = commentOptional.get();
        
        return user.equals(comment.getUser());
    }
}