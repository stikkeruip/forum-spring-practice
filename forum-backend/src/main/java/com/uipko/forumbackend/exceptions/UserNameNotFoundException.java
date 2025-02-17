package com.uipko.forumbackend.exceptions;

public class UserNameNotFoundException extends RuntimeException {
    public UserNameNotFoundException(String user) {
        super("username" + user + " not found");
    }
}
