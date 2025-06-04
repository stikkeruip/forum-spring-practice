package com.uipko.forumbackend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        List<ValidationError> errors
) {
    public static ErrorResponseDto of(String type, String title, int status, String detail, String instance) {
        return new ErrorResponseDto(type, title, status, detail, instance, LocalDateTime.now(), null);
    }

    public static ErrorResponseDto of(String type, String title, int status, String detail, String instance, List<ValidationError> errors) {
        return new ErrorResponseDto(type, title, status, detail, instance, LocalDateTime.now(), errors);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValidationError(
            String field,
            Object rejectedValue,
            String message
    ) {}
}