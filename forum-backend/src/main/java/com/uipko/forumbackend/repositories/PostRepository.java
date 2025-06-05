package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByIdAndDeletedDateIsNull(Long id);

    List<Post> findPostsByUserAndDeletedDateIsNullOrderByCreatedDateDescIdDesc(User user);

    List<Post> findAllByUserAndDeletedDateIsNotNullOrderByCreatedDateDescIdDesc(User user);

    List<Post> findAllByDeletedDateIsNullOrderByCreatedDateDescIdDesc();

    List<Post> findAllByDeletedDateIsNotNullOrderByCreatedDateDescIdDesc();
    
    @Query(value = "SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user " +
           "WHERE p.deletedDate IS NULL " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Post> findAllActivePostsWithRelations(Pageable pageable);
    
    /**
     * Get post IDs first for pagination, then fetch with relations
     * This avoids the N+1 query warning with collection fetches
     */
    @Query(value = "SELECT p.id FROM Post p " +
           "WHERE p.deletedDate IS NULL " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Long> findActivePostIds(Pageable pageable);
    
    /**
     * Fetch posts by IDs with all relationships loaded
     * Use this after getting paginated IDs to avoid N+1 queries
     */
    @EntityGraph(attributePaths = {"user", "deletedBy", "reactions", "reactions.user"})
    @Query(value = "SELECT DISTINCT p FROM Post p " +
           "WHERE p.id IN :postIds " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Post> findPostsByIdsWithRelations(@Param("postIds") List<Long> postIds);
    
    /**
     * Optimized query to get posts with comment counts - no pagination to avoid N+1 warning
     * Gets top posts for cache warming without EntityGraph pagination conflict
     */
    @Query(value = "SELECT p, " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p) as commentCount " +
           "FROM Post p " +
           "WHERE p.deletedDate IS NULL " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Object[]> findTopActivePostsWithCommentCounts();
    
    /**
     * Comprehensive relationship loading for cache warming
     * Fetches posts by IDs to avoid EntityGraph + Pageable conflicts
     */
    @EntityGraph(attributePaths = {
        "user", 
        "deletedBy", 
        "reactions", 
        "reactions.user",
        "comments",
        "comments.user"
    })
    @Query(value = "SELECT DISTINCT p FROM Post p " +
           "WHERE p.id IN :postIds " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Post> findPostsByIdsWithAllRelations(@Param("postIds") List<Long> postIds);
    
    /**
     * Get posts with minimal relationships for performance-critical operations
     */
    @Query(value = "SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.user " +
           "WHERE p.deletedDate IS NULL " +
           "ORDER BY p.createdDate DESC, p.id DESC")
    List<Post> findAllActivePostsMinimal(Pageable pageable);
    
    /**
     * Get single post with all relationships for caching using EntityGraph
     */
    @EntityGraph(attributePaths = {"user", "deletedBy", "reactions", "reactions.user", "comments", "comments.user"})
    @Query(value = "SELECT p FROM Post p " +
           "WHERE p.id = :postId AND p.deletedDate IS NULL")
    Optional<Post> findActivePostWithAllRelations(@Param("postId") Long postId);
}
