package com.uipko.forumbackend.exceptions;

public class UserNameExistsException extends RuntimeException {
    public UserNameExistsException(String user) {
        super("User name " + user + " already exists.");
    }
}
