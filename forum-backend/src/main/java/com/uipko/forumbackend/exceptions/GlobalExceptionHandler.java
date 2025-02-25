package com.uipko.forumbackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ================================
     * User Exception Handlers
     * ================================
     */

    @ExceptionHandler({IncorrectLoginException.class})
    public ResponseEntity<Object> handleIncorrectLoginException(IncorrectLoginException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }

    @ExceptionHandler({UserNameEmptyException.class})
    public ResponseEntity<Object> handleUserNameEmptyException(UserNameEmptyException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({UserNameExistsException.class})
    public ResponseEntity<Object> handleUserNameExistsException(UserNameExistsException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exception.getMessage());
    }

    @ExceptionHandler({UserNameNotEmptyException.class})
    public ResponseEntity<Object> handleUserNameNotEmptyException(UserNameNotEmptyException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }

    @ExceptionHandler({UserNameNotFoundException.class})
    public ResponseEntity<Object> handleUserNameNotFoundException(UserNameNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<Object> handleNameNotFoundException(UserNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception.getMessage());
    }

    /**
     * ================================
     * Post Exception Handlers
     * ================================
     */

    @ExceptionHandler({PostTitleEmptyException.class})
    public ResponseEntity<Object> handleNameNotFoundException(PostTitleEmptyException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({PostContentEmptyException.class})
    public ResponseEntity<Object> handleNameNotFoundException(PostContentEmptyException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({PostNotFoundException.class})
    public ResponseEntity<Object> handleNameNotFoundException(PostNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({PostUpdateTitleException.class})
    public ResponseEntity<Object> handleNameNotFoundException(PostUpdateTitleException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({PostDeleteUnauthorizedException.class})
    public ResponseEntity<Object> handleNameNotFoundException(PostDeleteUnauthorizedException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }

    /**
     * ================================
     * Comment Exception Handlers
     * ================================
     */

    @ExceptionHandler({CommentContentEmptyException.class})
    public ResponseEntity<Object> handleNameNotFoundException(CommentContentEmptyException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({CommentNotFoundException.class})
    public ResponseEntity<Object> handleNameNotFoundException(CommentNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }

    @ExceptionHandler({CommentDeleteUnauthorizedException.class})
    public ResponseEntity<Object> handleNameNotFoundException(CommentDeleteUnauthorizedException exception) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());
    }
}
