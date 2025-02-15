package com.uipko.forumbackend.exceptions;

public class IncorrectLoginException extends RuntimeException {
    public IncorrectLoginException() {
        super("Login details are incorrect");
    }
}
