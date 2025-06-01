package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.*;
import com.uipko.forumbackend.domain.entities.User;

import java.util.List;

public interface UserMapper {

    User registerDtoToUser(UserRegisterDto userRegisterDto);

    UserRegisterResponseDto userToRegisterResponseDto(User user);

    User loginDtoToUser(UserLoginDto userLoginDto);

    UserLoginResponseDto userToLoginResponseDto(User user, String token);

    UserProfileResponseDto userToProfileResponseDto(User user, List<PostResponseDto> posts);
}
