package com.uipko.forumbackend.controllers;

import com.uipko.forumbackend.domain.dto.PostResponseDto;
import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserProfileResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.Post;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.exceptions.UserNotFoundException;
import com.uipko.forumbackend.mappers.PostMapper;
import com.uipko.forumbackend.mappers.UserMapper;
import com.uipko.forumbackend.repositories.PostRepository;
import com.uipko.forumbackend.repositories.UserRepository;
import com.uipko.forumbackend.security.util.CurrentUserProvider;
import com.uipko.forumbackend.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserMapper userMapper, PostRepository postRepository, 
                          PostMapper postMapper, CurrentUserProvider currentUserProvider, UserRepository userRepository) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    @PostMapping(path = "/register")
    public UserRegisterResponseDto registerUser(@RequestBody UserRegisterDto userRegisterDto) {
        logger.info("User registration attempt: {}", userRegisterDto.name());
        User user = userService.registerUser(
                userMapper.registerDtoToUser(userRegisterDto)
        );
        logger.info("User registered successfully: {}", user.getName());
        return userMapper.userToRegisterResponseDto(user);
    }

    @PostMapping(path = "/login", produces = "text/plain")
    @ResponseBody
    public String loginUser(@RequestBody UserLoginDto userLoginDto) {
        logger.info("User login attempt: {}", userLoginDto.name());
        String token = userService.loginUser(userMapper.loginDtoToUser(userLoginDto));
        logger.info("User logged in successfully: {}", userLoginDto.name());
        return token;
    }

    @GetMapping(path = "/profile")
    public UserProfileResponseDto userProfile() {
        User user = currentUserProvider.getAuthenticatedUser();
        logger.info("Profile accessed by user: {}", user.getName());
        
        // Get non-deleted posts
        List<Post> userPosts = postRepository.findPostsByUserAndDeletedDateIsNullOrderByCreatedDateDescIdDesc(user);
        List<PostResponseDto> postResponseDtos = userPosts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());
        
        // Get deleted posts - users can see their own deleted posts, admins and moderators can see all
        List<Post> deletedPosts = postRepository.findAllByUserAndDeletedDateIsNotNullOrderByCreatedDateDescIdDesc(user);
        List<PostResponseDto> deletedPostResponseDtos = deletedPosts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());

        return userMapper.userToProfileResponseDto(user, postResponseDtos, deletedPostResponseDtos);
    }

    @GetMapping(path = "/users/exists")
    public Map<String, Boolean> checkUserExists(@RequestParam String username) {
        boolean exists = userRepository.existsByName(username);
        return Map.of("exists", exists);
    }

    @GetMapping(path = "/users/{username}/profile")
    public UserProfileResponseDto getUserProfile(@PathVariable String username) {
        logger.info("Public profile accessed for user: {}", username);
        User user = userRepository.findUserByName(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        
        // Get non-deleted posts - public can see these
        List<Post> userPosts = postRepository.findPostsByUserAndDeletedDateIsNullOrderByCreatedDateDescIdDesc(user);
        List<PostResponseDto> postResponseDtos = userPosts.stream()
                .map(postMapper::postToResponseDto)
                .collect(Collectors.toList());
        
        // Check if the requester is authenticated and is either the user, admin, or moderator
        User currentUser = null;
        try {
            currentUser = currentUserProvider.getAuthenticatedUser();
        } catch (Exception e) {
            // User is not authenticated, that's okay for public profiles
        }
        
        List<PostResponseDto> deletedPostResponseDtos = null;
        if (currentUser != null && 
            (currentUser.getName().equals(username) || 
             "ADMIN".equals(currentUser.getRole()) || 
             "MODERATOR".equals(currentUser.getRole()))) {
            // Get deleted posts only if viewer is the user themselves, admin, or moderator
            List<Post> deletedPosts = postRepository.findAllByUserAndDeletedDateIsNotNullOrderByCreatedDateDescIdDesc(user);
            deletedPostResponseDtos = deletedPosts.stream()
                    .map(postMapper::postToResponseDto)
                    .collect(Collectors.toList());
        }

        return userMapper.userToProfileResponseDto(user, postResponseDtos, deletedPostResponseDtos);
    }
}
