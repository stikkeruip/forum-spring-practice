package com.uipko.forumbackend.exceptions;

public class RateLimitExceededException extends ForumBusinessException {
    
    public RateLimitExceededException(String operation) {
        super("Rate limit exceeded for operation: " + operation + ". Please try again later.");
    }
    
    public RateLimitExceededException(String operation, String details) {
        super("Rate limit exceeded for " + operation + ": " + details);
    }
}