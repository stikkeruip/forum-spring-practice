package com.uipko.forumbackend.exceptions;

public class PostTitleEmptyException extends RuntimeException {
    public PostTitleEmptyException() {
        super("Title can not be empty");
    }
}
