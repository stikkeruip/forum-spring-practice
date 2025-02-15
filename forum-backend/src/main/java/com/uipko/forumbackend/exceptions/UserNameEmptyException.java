package com.uipko.forumbackend.exceptions;

public class UserNameEmptyException extends RuntimeException {
    public UserNameEmptyException() {
        super("User name is empty.");
    }
}
