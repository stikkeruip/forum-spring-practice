package com.uipko.forumbackend.exceptions;

public class FriendshipNotFoundException extends ForumBusinessException {
    public FriendshipNotFoundException(Long friendshipId) {
        super("Friendship with ID " + friendshipId + " not found.");
    }
    
    public FriendshipNotFoundException(String user1, String user2) {
        super("No friendship found between " + user1 + " and " + user2 + ".");
    }
}