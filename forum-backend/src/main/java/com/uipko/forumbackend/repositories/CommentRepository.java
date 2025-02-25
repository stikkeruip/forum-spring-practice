package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndDeletedDateIsNull(Long id);

    List<Comment> getCommentsByUserNameAndDeletedDateIsNull(String userName);

    List<Comment> getCommentsByPostIdAndDeletedDateIsNull(Long postId);
}
