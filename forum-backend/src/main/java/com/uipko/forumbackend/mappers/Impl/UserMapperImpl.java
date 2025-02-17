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
    public User registerDtoToUser(UserRegisterDto userRegisterDto) {
        return new User(
                userRegisterDto.name(),
                userRegisterDto.password()
        );
    }

    @Override
    public UserRegisterResponseDto userToRegisterResponseDto(User user) {
        return new UserRegisterResponseDto(
                user.getName()
        );
    }

    @Override
    public User loginDtoToUser(UserLoginDto userLoginDto) {
        return new User(
                userLoginDto.name(),
                userLoginDto.password()
        );
    }

    @Override
    public UserLoginResponseDto userToLoginResponseDto(User user, String token) {
        return new UserLoginResponseDto(
                user.getName(),
                token
        );
    }
}
