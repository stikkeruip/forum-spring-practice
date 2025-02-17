package com.uipko.forumbackend.exceptions;

public class UserNameNotEmptyException extends RuntimeException {
    public UserNameNotEmptyException() {
        super("User name is not empty.");
    }
}
