package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.PostContentEmptyException;
import com.uipko.forumbackend.exceptions.PostNotFoundException;
import com.uipko.forumbackend.exceptions.PostTitleEmptyException;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.PostService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();

        User user = userRepository.findUserByName(name).orElseThrow(() -> new UserNotFoundException(name));
        post.setUser(user);

        return postRepository.save(post);
    }

    @Override
    public Post getPost(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
    }

    @Override
    public Post updatePost(Post newPost) {
        Long postId = newPost.getId();
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId));

        if (newPost.getContent() == null || newPost.getContent().isBlank()) {
            throw new PostContentEmptyException();
        }

        post.setContent(newPost.getContent());
        post.setUpdatedDate(LocalDateTime.now());
        return postRepository.save(post);
    }

    @Transactional
    @Override
    public void deletePost(Post post) {
        postRepository.delete(post);
    }

    @Override
    public List<Post> getPostsByUser(User user) {
        return postRepository.findPostsByUser(user);
    }
}
