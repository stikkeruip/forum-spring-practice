package com.uipko.forumbackend.exceptions;

public class InvalidFriendResponseException extends ForumValidationException {
    public InvalidFriendResponseException(String response) {
        super("Invalid friend request response: " + response + ". Valid responses are 'ACCEPT' or 'DECLINE'.");
    }
}