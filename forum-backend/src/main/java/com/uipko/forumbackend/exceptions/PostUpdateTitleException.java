package com.uipko.forumbackend.exceptions;

public class PostUpdateTitleException extends RuntimeException {
    public PostUpdateTitleException() {
        super("Can not update post title");
    }
}
