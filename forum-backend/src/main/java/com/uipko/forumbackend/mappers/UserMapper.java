package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.UserLoginDto;
import com.uipko.forumbackend.domain.dto.UserLoginResponseDto;
import com.uipko.forumbackend.domain.dto.UserRegisterDto;
import com.uipko.forumbackend.domain.dto.UserRegisterResponseDto;
import com.uipko.forumbackend.domain.entities.User;

public interface UserMapper {

    User registerDtoToUser(UserRegisterDto userRegisterDto);

    UserRegisterResponseDto userToRegisterResponseDto(User user);

    User loginDtoToUser(UserLoginDto userLoginDto);

    UserLoginResponseDto userToLoginResponseDto(User user, String token);
}
