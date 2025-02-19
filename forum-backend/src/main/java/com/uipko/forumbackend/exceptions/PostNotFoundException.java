package com.uipko.forumbackend.exceptions;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long id) {
        super("Post with id " + id + " could not be found");
    }
}
