package com.uipko.forumbackend.exceptions;

public class PostContentEmptyException extends RuntimeException {
    public PostContentEmptyException() {
        super("Post content can not be empty");
    }
}
