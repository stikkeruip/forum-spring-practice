package com.uipko.forumbackend.exceptions;

public class CommentDeleteUnauthorizedException extends RuntimeException {
    public CommentDeleteUnauthorizedException(String userName) {
        super("User " + userName + " unauthorized to delete this comment");
    }
}
