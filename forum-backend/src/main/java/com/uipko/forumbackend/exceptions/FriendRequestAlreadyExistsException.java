package com.uipko.forumbackend.exceptions;

public class FriendRequestAlreadyExistsException extends ForumBusinessException {
    public FriendRequestAlreadyExistsException(String requester, String addressee) {
        super("A friend request between " + requester + " and " + addressee + " already exists.");
    }
}