package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.PostResponseDto;
import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserProfileResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, UserMapper userMapper, PostRepository postRepository, 
                          PostMapper postMapper, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(path = "/register")
    public UserRegisterResponseDto registerUser(@RequestBody UserRegisterDto userRegisterDto) {
        User user = userService.registerUser(
                userMapper.registerDtoToUser(userRegisterDto)
        );
        return userMapper.userToRegisterResponseDto(user);
    }

    @PostMapping(path = "/login", produces = "text/plain")
    @ResponseBody
    public String loginUser(@RequestBody UserLoginDto userLoginDto) {
        return userService.loginUser(userMapper.loginDtoToUser(userLoginDto));
    }

    @GetMapping(path = "/profile")
    public UserProfileResponseDto userProfile() {
        User user = currentUserProvider.getAuthenticatedUser();
        List<Post> userPosts = postRepository.findPostsByUserAndDeletedDateIsNull(user);
        List<PostResponseDto> postResponseDtos = userPosts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());

        return userMapper.userToProfileResponseDto(user, postResponseDtos);
    }
}
