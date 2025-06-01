package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.PostReaction;
import com.uipko.forumbackend.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostAndUser(Post post, User user);
    boolean existsByPostAndUser(Post post, User user);
}