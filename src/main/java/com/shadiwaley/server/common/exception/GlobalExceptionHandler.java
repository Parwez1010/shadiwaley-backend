package com.shadiwaley.server.common.exception;

import com.shadiwaley.server.common.response.ApiError;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError(
                        error.getField(),
                        "VALIDATION_ERROR",
                        error.getDefaultMessage()
                ))
                .toList();

        return ResponseFactory.failure("Validation failed", errors);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNotFound(EntityNotFoundException ex) {
        return ResponseFactory.failure(
                ex.getMessage(),
                List.of(new ApiError(null, "NOT_FOUND", ex.getMessage()))
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleBadRequest(IllegalArgumentException ex) {
        return ResponseFactory.failure(
                ex.getMessage(),
                List.of(new ApiError(null, "BAD_REQUEST", ex.getMessage()))
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGeneric(Exception ex) {
        return ResponseFactory.failure(
                "Something went wrong",
                List.of(new ApiError(null, "INTERNAL_SERVER_ERROR", ex.getMessage()))
        );
    }
}