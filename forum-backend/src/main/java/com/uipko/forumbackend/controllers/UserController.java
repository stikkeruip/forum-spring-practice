package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping(path = "/register")
    public UserRegisterResponseDto registerUser(@RequestBody UserRegisterDto userRegisterDto) {
        User user = userService.registerUser(
                userMapper.registerDtoToUser(userRegisterDto)
        );
        return userMapper.userToRegisterResponseDto(user);
    }

    @PostMapping(path = "/login")
    @ResponseBody
    public String loginUser(@RequestBody UserLoginDto userLoginDto) {
        return userService.loginUser(userMapper.loginDtoToUser(userLoginDto));
    }

    @GetMapping(path = "/profile")
    public String userProfile() {
        return "Hello";
    }
}