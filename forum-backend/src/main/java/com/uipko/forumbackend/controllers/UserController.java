package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserLoginResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.IncorrectLoginException;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.services.JwtService;
import com.uipko.forumbackend.services.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public UserController(UserService userService,
                          UserMapper userMapper,
                          JwtService jwtService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    @PostMapping(path = "/register")
    public UserRegisterResponseDto registerUser(@RequestBody UserRegisterDto userRegisterDto) {
        User user = userService.registerUser(
                userMapper.fromRegisterDto(userRegisterDto)
        );
        return userMapper.toRegisterResponseDto(user);
    }

    @PostMapping(path = "/login")
    public UserLoginResponseDto loginUser(@RequestBody UserLoginDto userLoginDto,
                                          HttpServletResponse response) {

        // userService.loginUser returns a Map<User, Boolean>
        Map<User, Boolean> loginResult = userService.loginUser(
                userMapper.fromLoginDto(userLoginDto)
        );
        User user = loginResult.keySet().iterator().next();
        Boolean isAuthenticated = loginResult.get(user);

        if (!isAuthenticated) {
            throw new IncorrectLoginException();
        }

        // Generate JWT and set as a cookie
        String token = jwtService.generateToken(user.getName());

        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production (HTTPS only)
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60 * 10); // 10 hours (in seconds)
        response.addCookie(jwtCookie);

        return userMapper.toLoginResponseDto(user, token);
    }
}