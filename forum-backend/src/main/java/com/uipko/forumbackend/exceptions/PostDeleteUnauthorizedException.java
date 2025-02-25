package com.uipko.forumbackend.exceptions;

public class PostDeleteUnauthorizedException extends RuntimeException {
    public PostDeleteUnauthorizedException(String userName) {
        super("User " + userName + " unauthorized to delete this post");
    }
}
