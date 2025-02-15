package com.uipko.forumbackend.exceptions;

public class UserIdNotEmptyException extends RuntimeException {
    public UserIdNotEmptyException() {
        super("User id is not empty.");
    }
}
