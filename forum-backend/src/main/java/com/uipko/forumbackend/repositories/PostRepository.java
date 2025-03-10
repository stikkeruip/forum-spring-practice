package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findByIdAndDeletedDateIsNull(Long id);

    List<Post> findPostsByUserAndDeletedDateIsNull(User user);
}
