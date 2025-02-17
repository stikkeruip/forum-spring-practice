package com.uipko.forumbackend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
}
