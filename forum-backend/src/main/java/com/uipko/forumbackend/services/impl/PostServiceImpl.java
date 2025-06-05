package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.dto.cache.CacheablePostDto;
import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.PostReaction;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.domain.events.PostEvent;
import com.uipko.forumbackend.exceptions.PostContentEmptyException;
import com.uipko.forumbackend.exceptions.PostDeleteUnauthorizedException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.exceptions.PostTitleEmptyException;
import com.uipko.forumbackend.mappers.CacheMapper;
import com.uipko.forumbackend.repositories.CommentRepository;
import com.uipko.forumbackend.repositories.PostReactionRepository;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.NotificationService;
import com.uipko.forumbackend.services.PostService;
import com.uipko.forumbackend.services.RedisMessagingService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisMessagingService redisMessagingService;
    private final CacheMapper cacheMapper;

    public PostServiceImpl(PostRepository postRepository, PostReactionRepository postReactionRepository, 
                          CommentRepository commentRepository, CurrentUserProvider currentUserProvider,
                          NotificationService notificationService, RedisTemplate<String, Object> redisTemplate,
                          ApplicationEventPublisher eventPublisher, RedisMessagingService redisMessagingService,
                          CacheMapper cacheMapper) {
        this.postRepository = postRepository;
        this.postReactionRepository = postReactionRepository;
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.redisMessagingService = redisMessagingService;
        this.cacheMapper = cacheMapper;
    }

    @Transactional
    @Override
    @CacheEvict(value = "posts", allEntries = true) // Clear all posts cache when new post is created
    public Post createPost(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new PostTitleEmptyException();
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        User user = currentUserProvider.getAuthenticatedUser();
        post.setUser(user);

        Post savedPost = postRepository.save(post);
        
        // Warm up the cache for the new post using safe DTO to prevent lazy loading issues
        CacheablePostDto cacheablePost = cacheMapper.toDto(savedPost, 0); // New post has 0 comments
        redisTemplate.opsForValue().set(
            "posts::post:" + savedPost.getId(), 
            cacheablePost, 
            Duration.ofMinutes(15)
        );
        
        // Publish event for real-time updates
        PostEvent postEvent = PostEvent.created(savedPost.getId(), user.getName(), savedPost.getTitle());
        eventPublisher.publishEvent(postEvent);
        redisMessagingService.publishPostEvent(postEvent);
        
        log.info("Created new post with ID: {} by user: {}", savedPost.getId(), user.getName());
        return savedPost;
    }

    @Override
    public Post getPost(Long id) {
        String cacheKey = "posts::post:" + id;
        
        // Try to get from cache first
        CacheablePostDto cachedDto = (CacheablePostDto) redisTemplate.opsForValue().get(cacheKey);
        if (cachedDto != null) {
            log.debug("Cache hit - retrieving post {} from cache", id);
            // For cached posts, we still need to get the entity for business logic
            // But we can verify it matches cached data
            Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
            checkPostAccess(post, id);
            return post;
        }
        
        log.debug("Cache miss - fetching post {} from database", id);
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        
        checkPostAccess(post, id);
        
        // Get comment count for caching
        Integer commentCount = commentRepository.countCommentsByPostIdAndDeletedDateIsNull(id);
        
        // Cache the DTO to prevent lazy loading issues
        CacheablePostDto cacheablePost = cacheMapper.toDto(post, commentCount);
        redisTemplate.opsForValue().set(cacheKey, cacheablePost, Duration.ofMinutes(15));
        
        // Cache post reaction counts for quick access
        cachePostReactionCounts(post);
        
        return post;
    }
    
    private void checkPostAccess(Post post, Long id) {
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
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "posts", key = "'post:' + #newPost.id"),
        @CacheEvict(value = "posts", allEntries = true)
    })
    public Post updatePost(Post newPost) {
        Long postId = newPost.getId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (newPost.getContent() == null || newPost.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        post.setContent(newPost.getContent());
        post.setUpdatedDate(newPost.getUpdatedDate());
        Post updatedPost = postRepository.save(post);
        
        log.info("Updated post with ID: {}", postId);
        return updatedPost;
    }

    @Transactional
    @Override
    @Caching(evict = {
        @CacheEvict(value = "posts", key = "'post:' + #id"),
        @CacheEvict(value = "posts", allEntries = true)
    })
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

        // Remove from all relevant caches
        evictPostCaches(id);

        // Publish event for real-time updates
        PostEvent deleteEvent = PostEvent.deleted(id, user.getName());
        eventPublisher.publishEvent(deleteEvent);
        redisMessagingService.publishPostEvent(deleteEvent);

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
        
        log.info("Deleted post with ID: {} by user: {}", id, user.getName());
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
        String cacheKey = "posts::all-posts";
        
        // Try to get from cache first
        @SuppressWarnings("unchecked")
        List<CacheablePostDto> cachedDtos = (List<CacheablePostDto>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedDtos != null) {
            log.debug("Cache hit - retrieving all posts from cache");
            // For cached posts, we still need to get entities for business logic
            return getAllPostsFromDatabase();
        }
        
        log.debug("Cache miss - fetching all posts from database");
        List<Post> posts = getAllPostsFromDatabase();
        
        // Cache the DTOs to prevent lazy loading issues
        List<CacheablePostDto> cacheablePosts = posts.stream()
                .map(post -> {
                    Integer commentCount = commentRepository.countCommentsByPostIdAndDeletedDateIsNull(post.getId());
                    return cacheMapper.toDto(post, commentCount);
                })
                .toList();
        
        redisTemplate.opsForValue().set(cacheKey, cacheablePosts, Duration.ofMinutes(10));
        
        return posts;
    }
    
    private List<Post> getAllPostsFromDatabase() {
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
        
        log.info("Restored post with ID: {} by user: {}", id, currentUser.getName());
    }

    /**
     * Cache post reaction counts for quick access
     */
    private void cachePostReactionCounts(Post post) {
        String reactionsKey = "reactions:post:" + post.getId();
        redisTemplate.opsForHash().put(reactionsKey, "likes", post.getLikes());
        redisTemplate.opsForHash().put(reactionsKey, "dislikes", post.getDislikes());
        redisTemplate.expire(reactionsKey, Duration.ofMinutes(10));
    }

    /**
     * Remove post from all relevant caches
     */
    private void evictPostCaches(Long postId) {
        // Remove individual post cache
        redisTemplate.delete("posts::post:" + postId);
        
        // Remove reaction counts cache
        redisTemplate.delete("reactions:post:" + postId);
        
        // Remove from trending posts if exists
        redisTemplate.opsForZSet().remove("trending_posts", postId.toString());
        
        log.debug("Evicted caches for post ID: {}", postId);
    }
}
