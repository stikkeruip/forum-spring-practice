package com.uipko.forumbackend.security.util;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();
        return userRepository.findUserByName(name).orElseThrow(() -> new UserNotFoundException(name));
    }
}
