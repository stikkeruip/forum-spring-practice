package com.uipko.forumbackend.mappers.Impl;

import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserLoginResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.User;
import com.uipko.forumbackend.mappers.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User fromRegisterDto(UserRegisterDto userRegisterDto) {
        return new User(
                userRegisterDto.id(),
                userRegisterDto.name(),
                userRegisterDto.password()
        );
    }

    @Override
    public UserRegisterDto toRegisterDto(User user) {
        return new UserRegisterDto(
                user.getId(),
                user.getName(),
                user.getPassword()
        );
    }

    @Override
    public UserRegisterResponseDto toRegisterResponseDto(User user) {
        return new UserRegisterResponseDto(
                user.getId(),
                user.getName()
        );
    }

    @Override
    public User fromLoginDto(UserLoginDto userLoginDto) {
        return new User(
                userLoginDto.id(),
                userLoginDto.name(),
                userLoginDto.password()
        );
    }

    @Override
    public UserLoginDto toLoginDto(User user) {
        return null;
    }

    @Override
    public UserLoginResponseDto toLoginResponseDto(User user, String token) {
        return new UserLoginResponseDto(
                user.getId(),
                user.getName(),
                token
        );
    }
}
