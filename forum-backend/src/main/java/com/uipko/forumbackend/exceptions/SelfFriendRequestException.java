package com.uipko.forumbackend.exceptions;

public class SelfFriendRequestException extends ForumValidationException {
    public SelfFriendRequestException() {
        super("Cannot send friend request to yourself.");
    }
}