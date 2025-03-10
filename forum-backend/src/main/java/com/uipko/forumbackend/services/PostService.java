package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;

import java.util.List;

public interface PostService {
    Post createPost(Post post);

    Post getPost(Long id);

    Post updatePost(Post newPost);

    void deletePost(Long id);

    List<Post> getPostsByUser(User user); //TODO change to userName
}
