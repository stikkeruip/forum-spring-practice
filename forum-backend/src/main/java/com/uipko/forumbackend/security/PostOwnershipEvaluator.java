package com.uipko.forumbackend.security;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Custom permission evaluator to check if a user is the owner of a post.
 * This is used in method security expressions like @PreAuthorize("hasRole('ADMIN') or @postOwnershipEvaluator.isOwner(authentication, #id)")
 */
@Component
public class PostOwnershipEvaluator {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostOwnershipEvaluator(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * Check if the authenticated user is the owner of the post with the given id.
     *
     * @param authentication the authentication object containing the user details
     * @param postId the id of the post to check ownership for
     * @return true if the user is the owner of the post, false otherwise
     */
    public boolean isOwner(Authentication authentication, Long postId) {
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findUserByName(username);
        Optional<Post> postOptional = postRepository.findById(postId);
        
        if (userOptional.isEmpty() || postOptional.isEmpty()) {
            return false;
        }
        
        User user = userOptional.get();
        Post post = postOptional.get();
        
        return user.equals(post.getUser());
    }
}