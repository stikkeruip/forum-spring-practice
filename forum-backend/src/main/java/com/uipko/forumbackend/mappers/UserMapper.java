package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserLoginResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.User;

public interface UserMapper {

    User fromRegisterDto(UserRegisterDto userRegisterDto);
    UserRegisterDto toRegisterDto(User user);

    UserRegisterResponseDto toRegisterResponseDto(User user);

    User fromLoginDto(UserLoginDto userLoginDto);
    UserLoginDto toLoginDto(User user);

    UserLoginResponseDto toLoginResponseDto(User user, String token);
}
