package com.uipko.forumbackend.exceptions;

import com.uipko.forumbackend.domain.dto.ErrorResponseDto;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://forum.example.com/problems";

    /**
     * ================================
     * User Exception Handlers
     * ================================
     */

    @ExceptionHandler(IncorrectLoginException.class)
    public ResponseEntity<ErrorResponseDto> handleIncorrectLoginException(
            IncorrectLoginException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/incorrect-login",
                "Incorrect Login Credentials",
                HttpStatus.UNAUTHORIZED.value(),
                "The provided username or password is incorrect",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UserNameEmptyException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNameEmptyException(
            UserNameEmptyException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/username-empty",
                "Username Required",
                HttpStatus.BAD_REQUEST.value(),
                "Username cannot be empty",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UserNameExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNameExistsException(
            UserNameExistsException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/username-exists",
                "Username Already Exists",
                HttpStatus.CONFLICT.value(),
                "A user with this username already exists",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserNameNotEmptyException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNameNotEmptyException(
            UserNameNotEmptyException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/username-not-empty",
                "Username Validation Error",
                HttpStatus.BAD_REQUEST.value(),
                "Username must be provided",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UserNameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNameNotFoundException(
            UserNameNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/username-not-found",
                "Username Not Found",
                HttpStatus.NOT_FOUND.value(),
                "The specified username does not exist",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/user-not-found",
                "User Not Found",
                HttpStatus.NOT_FOUND.value(),
                "The requested user does not exist",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * ================================
     * Post Exception Handlers
     * ================================
     */

    @ExceptionHandler(PostTitleEmptyException.class)
    public ResponseEntity<ErrorResponseDto> handlePostTitleEmptyException(
            PostTitleEmptyException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/post-title-empty",
                "Post Title Required",
                HttpStatus.BAD_REQUEST.value(),
                "Post title cannot be empty",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PostContentEmptyException.class)
    public ResponseEntity<ErrorResponseDto> handlePostContentEmptyException(
            PostContentEmptyException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/post-content-empty",
                "Post Content Required",
                HttpStatus.BAD_REQUEST.value(),
                "Post content cannot be empty",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlePostNotFoundException(
            PostNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/post-not-found",
                "Post Not Found",
                HttpStatus.NOT_FOUND.value(),
                "The requested post does not exist",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PostUpdateTitleException.class)
    public ResponseEntity<ErrorResponseDto> handlePostUpdateTitleException(
            PostUpdateTitleException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/post-title-update-error",
                "Post Title Update Error",
                HttpStatus.BAD_REQUEST.value(),
                "Post title cannot be updated with an empty value",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PostDeleteUnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handlePostDeleteUnauthorizedException(
            PostDeleteUnauthorizedException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/post-delete-unauthorized",
                "Unauthorized Post Deletion",
                HttpStatus.FORBIDDEN.value(),
                "You are not authorized to delete this post",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * ================================
     * Comment Exception Handlers
     * ================================
     */

    @ExceptionHandler(CommentContentEmptyException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentContentEmptyException(
            CommentContentEmptyException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/comment-content-empty",
                "Comment Content Required",
                HttpStatus.BAD_REQUEST.value(),
                "Comment content cannot be empty",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentNotFoundException(
            CommentNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/comment-not-found",
                "Comment Not Found",
                HttpStatus.NOT_FOUND.value(),
                "The requested comment does not exist",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CommentDeleteUnauthorizedException.class)
    public ResponseEntity<ErrorResponseDto> handleCommentDeleteUnauthorizedException(
            CommentDeleteUnauthorizedException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/comment-delete-unauthorized",
                "Unauthorized Comment Deletion",
                HttpStatus.FORBIDDEN.value(),
                "You are not authorized to delete this comment",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * ================================
     * Security Exception Handlers
     * ================================
     */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/access-denied",
                "Access Denied",
                HttpStatus.FORBIDDEN.value(),
                "You do not have permission to access this resource",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/authentication-failed",
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication is required to access this resource",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/bad-credentials",
                "Invalid Credentials",
                HttpStatus.UNAUTHORIZED.value(),
                "The provided credentials are invalid",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * ================================
     * JWT Exception Handlers
     * ================================
     */

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponseDto> handleExpiredJwtException(
            ExpiredJwtException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/jwt-expired",
                "JWT Token Expired",
                HttpStatus.UNAUTHORIZED.value(),
                "Your session has expired. Please log in again",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponseDto> handleMalformedJwtException(
            MalformedJwtException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/jwt-malformed",
                "Invalid JWT Token",
                HttpStatus.UNAUTHORIZED.value(),
                "The provided token is malformed",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponseDto> handleSignatureException(
            SignatureException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/jwt-signature-invalid",
                "Invalid JWT Signature",
                HttpStatus.UNAUTHORIZED.value(),
                "The token signature is invalid",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * ================================
     * Validation Exception Handlers
     * ================================
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponseDto.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/validation-failed",
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * ================================
     * Database Exception Handlers
     * ================================
     */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/data-integrity-violation",
                "Data Integrity Violation",
                HttpStatus.CONFLICT.value(),
                "The operation violates data integrity constraints",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/illegal-state",
                "Illegal State",
                HttpStatus.BAD_REQUEST.value(),
                "The requested operation cannot be performed in the current state",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * ================================
     * Generic Exception Handlers
     * ================================
     */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/illegal-argument",
                "Invalid Argument",
                HttpStatus.BAD_REQUEST.value(),
                "One or more arguments are invalid",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDto> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/internal-error",
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponseDto error = ErrorResponseDto.of(
                PROBLEM_BASE_URL + "/unexpected-error",
                "Unexpected Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * ================================
     * Helper Methods
     * ================================
     */

    private ErrorResponseDto.ValidationError mapFieldError(FieldError fieldError) {
        return new ErrorResponseDto.ValidationError(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage()
        );
    }
}