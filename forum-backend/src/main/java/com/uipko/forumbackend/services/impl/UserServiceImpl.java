package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.UserIdNotEmptyException;
import com.uipko.forumbackend.exceptions.UserNameEmptyException;
import com.uipko.forumbackend.exceptions.UserNameExistsException;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           AuthenticationManager authenticationManager,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User getUser(Long id) {
        return userRepository.findUserById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    @Override
    public User registerUser(User user) {
        if (user.getId() != null) {
            throw new UserIdNotEmptyException();
        }
        if (user.getName() == null || user.getName().isBlank()) {
            throw new UserNameEmptyException();
        }
        if (userRepository.existsByName(user.getName())) {
            throw new UserNameExistsException(user.getName());
        }

        // Encrypt the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(new User (
                null,
                user.getName(),
                user.getPassword())
        );
    }

    @Override
    public Map<User, Boolean> loginUser(User user) {
        // This uses the authentication manager to check the raw password
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(user.getName(), user.getPassword())
                );

        return Map.of(user, authentication.isAuthenticated());
    }

    @Transactional
    @Override
    public void deleteUser(Long id) {
        userRepository.delete(getUser(id));
    }
}