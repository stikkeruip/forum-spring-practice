package com.uipko.forumbackend.exceptions;

import jakarta.persistence.EntityNotFoundException;

public class UserNotFoundException extends EntityNotFoundException {
    public UserNotFoundException(String name) {
        super("User with name " + name + " not found.");
    }
}
