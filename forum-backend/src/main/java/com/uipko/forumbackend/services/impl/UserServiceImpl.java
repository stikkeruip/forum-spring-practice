package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.UserIdNotEmptyException;
import com.uipko.forumbackend.exceptions.UserNameEmptyException;
import com.uipko.forumbackend.exceptions.UserNameExistsException;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findUserById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @Override
    public User createUser(User user) {
        if (user.getId() != null) {
            throw new UserIdNotEmptyException();
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new UserNameEmptyException();
        }
        if (userRepository.existsByName(user.getName())) {
            throw new UserNameExistsException(user.getName());
        }

        return userRepository.save(new User (
                null,
                user.getName(),
                user.getPassword())
        );
    }

    @Transactional
    @Override
    public void deleteUser(Long id) {
        userRepository.delete(getUser(id));
    }
}
