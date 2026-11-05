package com.authbaseapp.authapp.Exception;

import com.authbaseapp.authapp.DTOs.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // Invalid DTO fields or malformed requests.
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    @ResponseStatus(BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(Exception ex) {
        String message = "Invalid request body or parameter";
        if (ex instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResponse.error(message);
    }

    // Services choose the status and message with ResponseStatusException.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(ex.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(CONFLICT)
    public ApiResponse<Void> handleDuplicate(DataIntegrityViolationException ex) {
        return ApiResponse.error("Email already in use");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse error) {
            return ResponseEntity.status(error.getStatusCode()).body(ApiResponse.error(error.getBody().getDetail()));
        }
        log.error("Unexpected request error", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred"));
    }
}
