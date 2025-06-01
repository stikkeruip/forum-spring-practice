package com.uipko.forumbackend.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class User {

    @Id
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "role", nullable = false)
    private String role = "USER";

    /**
     * Check if the user has a specific role.
     * 
     * @param roleName the role to check for
     * @return true if the user has the specified role, false otherwise
     */
    public boolean hasRole(String roleName) {
        return roleName.equals(role);
    }

    /**
     * Check if the user is an admin.
     * 
     * @return true if the user has the ADMIN role, false otherwise
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Check if the user is a moderator.
     * 
     * @return true if the user has the MODERATOR role, false otherwise
     */
    public boolean isModerator() {
        return "MODERATOR".equals(role);
    }
}
