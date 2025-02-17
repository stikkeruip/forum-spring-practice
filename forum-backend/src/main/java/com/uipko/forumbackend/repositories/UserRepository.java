package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository <User, String> {
    Boolean existsByName(String name);
    Optional<User> findUserByName(String name);
}
