package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.PostContentEmptyException;
import com.uipko.forumbackend.exceptions.PostDeleteUnauthorizedException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.exceptions.PostTitleEmptyException;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;

    public PostServiceImpl(PostRepository postRepository, CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    @Override
    public Post createPost(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new PostTitleEmptyException();
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        User user = currentUserProvider.getAuthenticatedUser();
        post.setUser(user);

        return postRepository.save(post);
    }

    @Override
    public Post getPost(Long id) {
        return postRepository.findByIdAndDeletedDateIsNull(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    @Override
    public Post updatePost(Post newPost) {
        Long postId = newPost.getId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (newPost.getContent() == null || newPost.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        post.setContent(newPost.getContent());
        post.setUpdatedDate(newPost.getUpdatedDate());
        return postRepository.save(post);
    }

    @Transactional
    @Override
    public void deletePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        User user = currentUserProvider.getAuthenticatedUser();

        if (!user.equals(post.getUser())) {
            throw new PostDeleteUnauthorizedException(user.getName());
        }

        post.setDeletedDate(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    public List<Post> getPostsByUser(User user) {
        return postRepository.findPostsByUserAndDeletedDateIsNull(user);
    }
}
