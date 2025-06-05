package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Friendship;
import com.uipko.forumbackend.domain.entities.Friendship.FriendshipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    /**
     * Find friendship between two users regardless of who initiated it.
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester.name = :user1 AND f.addressee.name = :user2) OR " +
           "(f.requester.name = :user2 AND f.addressee.name = :user1)")
    Optional<Friendship> findFriendshipBetweenUsers(@Param("user1") String user1, @Param("user2") String user2);
    
    /**
     * Find all accepted friendships for a user (both as requester and addressee).
     */
    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requester.name = :username OR f.addressee.name = :username) AND " +
           "f.status = :status")
    List<Friendship> findByUserAndStatus(@Param("username") String username, @Param("status") FriendshipStatus status);
    
    /**
     * Find pending friend requests sent to a user.
     */
    @EntityGraph(attributePaths = {"requester", "addressee"})
    @Query("SELECT f FROM Friendship f WHERE " +
           "f.addressee.name = :username AND f.status = :status")
    List<Friendship> findPendingRequestsForUser(@Param("username") String username, @Param("status") FriendshipStatus status);
    
    /**
     * Find pending friend requests sent by a user.
     */
    @Query("SELECT f FROM Friendship f WHERE " +
           "f.requester.name = :username AND f.status = :status")
    List<Friendship> findPendingRequestsByUser(@Param("username") String username, @Param("status") FriendshipStatus status);
    
    /**
     * Check if user can send friend request (no existing friendship or blocked status).
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
           "((f.requester.name = :requester AND f.addressee.name = :addressee) OR " +
           "(f.requester.name = :addressee AND f.addressee.name = :requester)) AND " +
           "f.status IN (:pending, :accepted, :blocked)")
    long countExistingFriendshipOrRequest(@Param("requester") String requester, @Param("addressee") String addressee, 
                                         @Param("pending") FriendshipStatus pending, 
                                         @Param("accepted") FriendshipStatus accepted, 
                                         @Param("blocked") FriendshipStatus blocked);
    
    /**
     * Get all friends (accepted friendships) with their online status.
     */
    @Query("SELECT f FROM Friendship f " +
           "JOIN FETCH f.requester r " +
           "JOIN FETCH f.addressee a " +
           "WHERE (f.requester.name = :username OR f.addressee.name = :username) AND " +
           "f.status = :status")
    List<Friendship> findAcceptedFriendsWithOnlineStatus(@Param("username") String username, @Param("status") FriendshipStatus status);
}