package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.PostReaction;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.PostContentEmptyException;
import com.uipko.forumbackend.exceptions.PostDeleteUnauthorizedException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.exceptions.PostTitleEmptyException;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.PostReactionRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;

    public PostServiceImpl(PostRepository postRepository, PostReactionRepository postReactionRepository, CommentRepository commentRepository, CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.postReactionRepository = postReactionRepository;
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    @Override
    public Post createPost(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new PostTitleEmptyException();
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        User user = currentUserProvider.getAuthenticatedUser();
        post.setUser(user);

        return postRepository.save(post);
    }

    @Override
    public Post getPost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

        // If post is deleted, check permissions
        if (post.getDeletedDate() != null) {
            User currentUser = currentUserProvider.getAuthenticatedUser();

            // Anonymous users can't access deleted posts
            if ("anonymousUser".equals(currentUser.getName())) {
                throw new PostNotFoundException(id);
            }

            // Allow access if user is the post owner, an admin, or a moderator
            boolean hasAccess = post.getUser().getName().equals(currentUser.getName()) || 
                               currentUser.isAdmin() || 
                               currentUser.isModerator();

            if (!hasAccess) {
                throw new PostNotFoundException(id);
            }
        }

        return post;
    }

    @Override
    public Post updatePost(Post newPost) {
        Long postId = newPost.getId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (newPost.getContent() == null || newPost.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        post.setContent(newPost.getContent());
        post.setUpdatedDate(newPost.getUpdatedDate());
        return postRepository.save(post);
    }

    @Transactional
    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @postOwnershipEvaluator.isOwner(authentication, #id)")
    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

        // Check if post is already deleted
        if (post.getDeletedDate() != null) {
            throw new IllegalStateException("Post is already deleted");
        }

        User user = currentUserProvider.getAuthenticatedUser();

        // Get the current time for consistent deletion timestamp
        LocalDateTime now = LocalDateTime.now();

        // Soft delete the post
        post.setDeletedDate(now);
        postRepository.save(post);

        // Soft delete all comments associated with this post
        List<Comment> comments = commentRepository.getCommentsByPostIdAndDeletedDateIsNull(id);
        for (Comment comment : comments) {
            comment.setDeletedDate(now);
        }
        commentRepository.saveAll(comments);
    }

    @Override
    public List<Post> getPostsByUser(User user) {
        User currentUser = currentUserProvider.getAuthenticatedUser();

        // If current user is admin or moderator, or if current user is the owner of the posts,
        // return all posts including deleted ones
        if (currentUser.isAdmin() || currentUser.isModerator() || currentUser.getName().equals(user.getName())) {
            return postRepository.findAll().stream()
                    .filter(post -> post.getUser().getName().equals(user.getName()))
                    .toList();
        }

        // For other users, return only non-deleted posts
        return postRepository.findPostsByUserAndDeletedDateIsNull(user);
    }

    @Override
    public List<Post> getAllPosts() {
        User currentUser = currentUserProvider.getAuthenticatedUser();

        // If user is anonymous, return only non-deleted posts
        if ("anonymousUser".equals(currentUser.getName())) {
            return postRepository.findAllByDeletedDateIsNull();
        }

        // If user is admin or moderator, return all posts including deleted ones
        if (currentUser.isAdmin() || currentUser.isModerator()) {
            return postRepository.findAll();
        }

        // For regular users, return all non-deleted posts and their own deleted posts
        List<Post> nonDeletedPosts = postRepository.findAllByDeletedDateIsNull();
        List<Post> ownDeletedPosts = postRepository.findAllByUserAndDeletedDateIsNotNull(currentUser);

        // Combine the lists
        nonDeletedPosts.addAll(ownDeletedPosts);
        return nonDeletedPosts;
    }

    @Override
    public List<Post> getDeletedPosts() {
        User currentUser = currentUserProvider.getAuthenticatedUser();

        // Anonymous users can't access deleted posts
        if ("anonymousUser".equals(currentUser.getName())) {
            return List.of();
        }

        // Admins and moderators can see all deleted posts
        if (currentUser.isAdmin() || currentUser.isModerator()) {
            return postRepository.findAllByDeletedDateIsNotNull();
        }

        // Regular users can only see their own deleted posts
        return postRepository.findAllByUserAndDeletedDateIsNotNull(currentUser);
    }

    @Override
    public Post getDeletedPostById(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

        // Check if post is actually deleted
        if (post.getDeletedDate() == null) {
            throw new PostNotFoundException(id);
        }

        User currentUser = currentUserProvider.getAuthenticatedUser();

        // Anonymous users can't access deleted posts
        if ("anonymousUser".equals(currentUser.getName())) {
            throw new PostNotFoundException(id);
        }

        // Allow access if user is the post owner, an admin, or a moderator
        boolean hasAccess = post.getUser().getName().equals(currentUser.getName()) || 
                           currentUser.isAdmin() || 
                           currentUser.isModerator();

        if (!hasAccess) {
            throw new PostNotFoundException(id);
        }

        return post;
    }

    @Transactional
    @Override
    public Post reactToPost(Long postId, String reactionType) {
        Post post = postRepository.findByIdAndDeletedDateIsNull(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        User user = currentUserProvider.getAuthenticatedUser();

        // Check if user already reacted to this post
        Optional<PostReaction> existingReaction = postReactionRepository.findByPostAndUser(post, user);

        if (existingReaction.isPresent()) {
            PostReaction reaction = existingReaction.get();
            String oldReactionType = reaction.getReactionType();

            // If same reaction type, remove the reaction (toggle)
            if (oldReactionType.equals(reactionType)) {
                postReactionRepository.delete(reaction);

                // Update post counts
                if ("LIKE".equals(reactionType)) {
                    post.setLikes(post.getLikes() - 1);
                } else if ("DISLIKE".equals(reactionType)) {
                    post.setDislikes(post.getDislikes() - 1);
                }
            } else {
                // Change reaction type
                reaction.setReactionType(reactionType);
                reaction.setCreatedDate(LocalDateTime.now());
                postReactionRepository.save(reaction);

                // Update post counts
                if ("LIKE".equals(reactionType)) {
                    post.setLikes(post.getLikes() + 1);
                    post.setDislikes(post.getDislikes() - 1);
                } else if ("DISLIKE".equals(reactionType)) {
                    post.setDislikes(post.getDislikes() + 1);
                    post.setLikes(post.getLikes() - 1);
                }
            }
        } else {
            // Create new reaction
            PostReaction reaction = new PostReaction();
            reaction.setPost(post);
            reaction.setUser(user);
            reaction.setReactionType(reactionType);
            reaction.setCreatedDate(LocalDateTime.now());
            postReactionRepository.save(reaction);

            // Update post counts
            if ("LIKE".equals(reactionType)) {
                post.setLikes(post.getLikes() + 1);
            } else if ("DISLIKE".equals(reactionType)) {
                post.setDislikes(post.getDislikes() + 1);
            }
        }

        return postRepository.save(post);
    }
}
