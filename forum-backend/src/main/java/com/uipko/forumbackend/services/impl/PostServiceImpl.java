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
import com.uipko.forumbackend.services.NotificationService;
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
    private final NotificationService notificationService;

    public PostServiceImpl(PostRepository postRepository, PostReactionRepository postReactionRepository, 
                          CommentRepository commentRepository, CurrentUserProvider currentUserProvider,
                          NotificationService notificationService) {
        this.postRepository = postRepository;
        this.postReactionRepository = postReactionRepository;
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
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
    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        
        User currentUser = currentUserProvider.getAuthenticatedUser();
        
        // Check authorization: admin/moderator can delete any post, users can only delete their own
        boolean isOwner = post.getUser().getName().equals(currentUser.getName());
        boolean isAdminOrModerator = currentUser.isAdmin() || currentUser.isModerator();
        
        if (!isOwner && !isAdminOrModerator) {
            throw new PostDeleteUnauthorizedException("You are not authorized to delete this post");
        }

        // Check if post is already deleted
        if (post.getDeletedDate() != null) {
            throw new IllegalStateException("Post is already deleted");
        }

        User user = currentUserProvider.getAuthenticatedUser();

        // Get the current time for consistent deletion timestamp
        LocalDateTime now = LocalDateTime.now();

        // Soft delete the post
        post.setDeletedDate(now);
        post.setDeletedBy(user);
        postRepository.save(post);

        // Send notification if post was deleted by admin/moderator (not the owner)
        if (!user.getName().equals(post.getUser().getName()) && 
            (user.isAdmin() || user.isModerator())) {
            notificationService.createPostDeletionNotification(user, post.getUser(), post);
        }

        // Soft delete all comments associated with this post
        List<Comment> comments = commentRepository.getCommentsByPostIdAndDeletedDateIsNull(id);
        for (Comment comment : comments) {
            comment.setDeletedDate(now);
            // Send notification if comment was deleted by admin/moderator (not the owner)
            if (!user.getName().equals(comment.getUser().getName()) && 
                (user.isAdmin() || user.isModerator())) {
                notificationService.createCommentDeletionNotification(user, comment.getUser(), comment);
            }
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
        return postRepository.findPostsByUserAndDeletedDateIsNullOrderByCreatedDateDescIdDesc(user);
    }

    @Override
    public List<Post> getAllPosts() {
        User currentUser = currentUserProvider.getAuthenticatedUser();

        // If user is anonymous, return only non-deleted posts
        if ("anonymousUser".equals(currentUser.getName())) {
            return postRepository.findAllByDeletedDateIsNullOrderByCreatedDateDescIdDesc();
        }

        // For admin or moderator, return only non-deleted posts on home page
        // (They can access deleted posts via /posts/deleted endpoint)
        if (currentUser.isAdmin() || currentUser.isModerator()) {
            return postRepository.findAllByDeletedDateIsNullOrderByCreatedDateDescIdDesc();
        }

        // For regular users, return only non-deleted posts on home page
        // (Their own deleted posts are visible on their profile page)
        return postRepository.findAllByDeletedDateIsNullOrderByCreatedDateDescIdDesc();
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
            return postRepository.findAllByDeletedDateIsNotNullOrderByCreatedDateDescIdDesc();
        }

        // Regular users can only see their own deleted posts
        return postRepository.findAllByUserAndDeletedDateIsNotNullOrderByCreatedDateDescIdDesc(currentUser);
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
                    // Send notification for new like
                    notificationService.createLikeNotification(user, post.getUser(), post);
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
                // Send notification for new like
                notificationService.createLikeNotification(user, post.getUser(), post);
            } else if ("DISLIKE".equals(reactionType)) {
                post.setDislikes(post.getDislikes() + 1);
            }
        }

        return postRepository.save(post);
    }

    @Transactional
    @Override
    public void restorePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));

        // Check if post is actually deleted
        if (post.getDeletedDate() == null) {
            throw new IllegalStateException("Post is not deleted");
        }

        User currentUser = currentUserProvider.getAuthenticatedUser();

        // Check authorization
        boolean isOwner = post.getUser().getName().equals(currentUser.getName());
        boolean isAdminOrModerator = currentUser.isAdmin() || currentUser.isModerator();

        if (isOwner && post.getDeletedBy() != null && !post.getDeletedBy().getName().equals(currentUser.getName())) {
            // User can't restore if admin/moderator deleted it
            throw new PostDeleteUnauthorizedException("You cannot restore a post deleted by an administrator or moderator");
        } else if (!isOwner && !isAdminOrModerator) {
            // Non-owners need admin/moderator role
            throw new PostDeleteUnauthorizedException("You are not authorized to restore this post");
        }

        // Restore the post
        post.setDeletedDate(null);
        post.setDeletedBy(null);
        postRepository.save(post);

        // Send notification if post was restored by admin/moderator (not the owner)
        if (!currentUser.getName().equals(post.getUser().getName()) && 
            (currentUser.isAdmin() || currentUser.isModerator())) {
            notificationService.createPostRestorationNotification(currentUser, post.getUser(), post);
        }

        // Restore all comments associated with this post
        List<Comment> comments = commentRepository.findAllByPost_Id(id);
        for (Comment comment : comments) {
            if (comment.getDeletedDate() != null) {
                comment.setDeletedDate(null);
            }
        }
        commentRepository.saveAll(comments);
    }
}
