package com.uipko.forumbackend.exceptions;

public abstract class ForumBusinessException extends ForumBaseException {
    
    public ForumBusinessException(String message) {
        super(message);
    }
    
    public ForumBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}