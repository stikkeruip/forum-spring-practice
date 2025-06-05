package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByRecipientNameOrderByCreatedDateDesc(String username, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.name = :username AND n.read = false")
    long countUnreadByRecipientUsername(@Param("username") String username);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids AND n.recipient.name = :username")
    void markAsReadByIdsAndUsername(@Param("ids") List<Long> ids, @Param("username") String username);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.name = :username")
    void markAllAsReadByUsername(@Param("username") String username);
    
    List<Notification> findTop50ByRecipientNameOrderByCreatedDateDesc(String username);
    
    @Query(value = "SELECT n FROM Notification n " +
           "LEFT JOIN FETCH n.recipient " +
           "LEFT JOIN FETCH n.actor " +
           "LEFT JOIN FETCH n.targetPost " +
           "LEFT JOIN FETCH n.targetComment " +
           "WHERE n.recipient.name = :username " +
           "ORDER BY n.createdDate DESC",
           countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.recipient.name = :username")
    List<Notification> findTop50ByRecipientNameWithRelationsOrderByCreatedDateDesc(@Param("username") String username, Pageable pageable);
    
    /**
     * Ultra-optimized query for cache warming using EntityGraph
     * Prevents LazyInitializationException and N+1 queries by loading all relationships
     * Uses @EntityGraph for maximum efficiency in batch loading
     */
    @EntityGraph(attributePaths = {
        "recipient", 
        "actor", 
        "targetPost", 
        "targetPost.user",
        "targetComment", 
        "targetComment.user"
    })
    @Query(value = "SELECT DISTINCT n FROM Notification n " +
           "WHERE n.recipient.name = :username " +
           "ORDER BY n.createdDate DESC")
    List<Notification> findNotificationsForCaching(@Param("username") String username, Pageable pageable);
}