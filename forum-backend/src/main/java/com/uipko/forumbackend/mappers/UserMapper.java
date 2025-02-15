package com.uipko.forumbackend.mappers;

import com.uipko.forumbackend.domain.dto.UserCreationDto;
import com.uipko.forumbackend.domain.dto.UserResponseDto;
import com.uipko.forumbackend.domain.entities.User;

public interface UserMapper {

    User fromCreationDto (UserCreationDto userCreationDto);
    UserCreationDto toCreationDto (User user);

    User fromResponseDto (UserResponseDto userResponseDto);
    UserResponseDto toResponseDto (User user);
}
