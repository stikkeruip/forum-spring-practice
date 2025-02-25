package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.User;

public interface UserService {
    User getUser(String name);

    User registerUser(User user);

    String loginUser(User user);

    void deleteUser(String userName); //TODO Make soft delete
}