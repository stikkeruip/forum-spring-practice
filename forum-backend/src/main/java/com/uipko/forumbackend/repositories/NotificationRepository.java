package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}