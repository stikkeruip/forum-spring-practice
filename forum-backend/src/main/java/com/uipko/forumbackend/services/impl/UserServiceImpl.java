package com.uipko.forumbackend.services.impl;

import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.*;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.services.JwtService;
import com.uipko.forumbackend.services.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository,
                           AuthenticationManager authenticationManager,
                           BCryptPasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public User getUser(String name) {
        return userRepository.findUserByName(name).orElseThrow(() -> new UserNotFoundException(name));
    }

    @Transactional
    @Override
    public User registerUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new UserNameEmptyException();
        }
        if (userRepository.existsByName(user.getName())) {
            throw new UserNameExistsException(user.getName());
        }

        return userRepository.save(new User (
                user.getName(),
                passwordEncoder.encode(user.getPassword()))
        );
    }

    @Override
    public String loginUser(User user) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getName(), user.getPassword())
            );
            return jwtService.generateToken(user.getName());
        } catch (AuthenticationException ex) {
            throw new IncorrectLoginException();
        }
    }

    @Transactional
    @Override
    public void deleteUser(String name) {
        userRepository.delete(getUser(name));
    }
}