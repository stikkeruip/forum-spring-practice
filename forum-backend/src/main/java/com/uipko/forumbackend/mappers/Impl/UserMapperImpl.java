package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    public UserProfileResponseDto userToProfileResponseDto(User user, List<PostResponseDto> posts) {
        UserProfileResponseDto dto = new UserProfileResponseDto();
        dto.setUsername(user.getName());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setPosts(posts);
        return dto;
    }
}
