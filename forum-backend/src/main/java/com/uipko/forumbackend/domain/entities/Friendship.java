package com.uipko.forumbackend.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friendships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_username", referencedColumnName = "name", nullable = false)
    private User requester;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_username", referencedColumnName = "name", nullable = false)
    private User addressee;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;
    
    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @UpdateTimestamp
    @Column(name = "updated_date", nullable = false)
    private LocalDateTime updatedDate;
    
    public enum FriendshipStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        BLOCKED
    }
    
    /**
     * Check if this friendship represents an accepted friendship between two users.
     */
    public boolean isAccepted() {
        return status == FriendshipStatus.ACCEPTED;
    }
    
    /**
     * Check if this friendship is pending approval.
     */
    public boolean isPending() {
        return status == FriendshipStatus.PENDING;
    }
    
    /**
     * Get the other user in this friendship relationship.
     */
    public User getOtherUser(User currentUser) {
        if (requester.getName().equals(currentUser.getName())) {
            return addressee;
        } else if (addressee.getName().equals(currentUser.getName())) {
            return requester;
        }
        throw new IllegalArgumentException("User is not part of this friendship");
    }
}