package com.uipko.forumbackend.repositories;

import com.uipko.forumbackend.domain.entities.Comment;
import com.uipko.forumbackend.domain.entities.CommentReaction;
import com.uipko.forumbackend.domain.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentAndUser(Comment comment, User user);
    boolean existsByCommentAndUser(Comment comment, User user);
}