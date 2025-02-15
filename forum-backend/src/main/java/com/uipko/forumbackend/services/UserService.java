package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.User;

import java.util.Optional;

public interface UserService {
    User getUser (Long id);
    User createUser(User user);
    void deleteUser (Long id);
}
