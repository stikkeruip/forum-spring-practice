package com.uipko.forumbackend.exceptions;

public abstract class ForumBaseException extends RuntimeException {
    
    public ForumBaseException(String message) {
        super(message);
    }
    
    public ForumBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}