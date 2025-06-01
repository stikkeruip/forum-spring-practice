package com.uipko.forumbackend.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactionDto {
    private String reactionType; // "LIKE" or "DISLIKE"
}