package com.uipko.forumbackend.exceptions;

public class CommentContentEmptyException extends RuntimeException {
    public CommentContentEmptyException() {
        super("Comment can not be empty");
    }
}
