package com.uipko.forumbackend.exceptions;

public abstract class ForumValidationException extends ForumBaseException {
    
    public ForumValidationException(String message) {
        super(message);
    }
    
    public ForumValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}