package com.uipko.forumbackend.services;

import com.uipko.forumbackend.domain.entities.User;

import java.util.Map;

public interface UserService {
    User getUser (Long id);
    User registerUser (User user);
    Map<User, Boolean> loginUser (User user);
    void deleteUser (Long id);
}
