package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.UserCreationDto;
import com.uipko.forumbackend.domain.dto.UserResponseDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public UserResponseDto createUser(@RequestBody UserCreationDto userCreationDto) {
        User createdUser = userService.createUser(userMapper.fromCreationDto(userCreationDto));

        return userMapper.toResponseDto(createdUser);
    }
}
