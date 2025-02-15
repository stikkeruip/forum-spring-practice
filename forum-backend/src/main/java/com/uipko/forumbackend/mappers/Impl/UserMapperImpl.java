package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.UserCreationDto;
import com.uipko.forumbackend.domain.dto.UserResponseDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User fromCreationDto(UserCreationDto userCreationDto) {
        return new User(
                userCreationDto.id(),
                userCreationDto.name(),
                userCreationDto.password()
        );
    }

    @Override
    public UserCreationDto toCreationDto(User user) {
        return new UserCreationDto(
                user.getId(),
                user.getName(),
                user.getPassword()
        );
    }

    @Override
    public User fromResponseDto(UserResponseDto userResponseDto) {
        return new User(
                userResponseDto.id(),
                userResponseDto.name(),
                null
        );
    }

    @Override
    public UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName()
        );
    }
}
