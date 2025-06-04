package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User registerDtoToUser(UserRegisterDto userRegisterDto) {
        User user = new User();
        user.setName(userRegisterDto.name());
        user.setPassword(userRegisterDto.password());
        user.setCreatedDate(LocalDateTime.now());
        return user;
    }

    @Override
    public UserRegisterResponseDto userToRegisterResponseDto(User user) {
        return new UserRegisterResponseDto(
                user.getName()
        );
    }

    @Override
    public User loginDtoToUser(UserLoginDto userLoginDto) {
        User user = new User();
        user.setName(userLoginDto.name());
        user.setPassword(userLoginDto.password());
        return user;
    }

    @Override
    public UserLoginResponseDto userToLoginResponseDto(User user, String token) {
        return new UserLoginResponseDto(
                user.getName(),
                token
        );
    }

    @Override
    public UserProfileResponseDto userToProfileResponseDto(User user, List<PostResponseDto> posts, List<PostResponseDto> deletedPosts) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setUsername(user.getName());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setPosts(posts);
        dto.setDeletedPosts(deletedPosts);
        return dto;
    }

    @Override
    public UserProfileResponseDto toProfileResponseDto(User user) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setUsername(user.getName());
        dto.setCreatedDate(user.getCreatedDate());
        // For online status display, we don't need posts
        dto.setPosts(List.of());
        dto.setDeletedPosts(List.of());
        return dto;
    }

    @Override
    public OnlineUserDto userToOnlineUserDto(User user) {
        OnlineUserDto dto = new OnlineUserDto();
        dto.setUsername(user.getName());
        // Convert LocalDateTime to ISO string format that matches frontend expectations
        dto.setCreatedDate(user.getCreatedDate().toString());
        // For online users list, we don't need the full posts data - keep it empty for performance
        dto.setPosts(List.of());
        dto.setDeletedPosts(List.of());
        return dto;
    }
}
