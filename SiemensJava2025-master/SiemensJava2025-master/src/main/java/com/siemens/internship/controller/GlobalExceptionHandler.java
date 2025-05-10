package com.siemens.internship.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.siemens.internship.model.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for standardized error responses across the application.
 *
 * This is a new class that was not in the original implementation.
 * It provides centralized exception handling for the entire application.
 *
 * BENEFITS:
 * 1. Standardizes error responses across all controllers
 * 2. Centralizes error handling logic
 * 3. Reduces code duplication
 * 4. Improves API user experience by providing consistent, detailed error information
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation exceptions from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Validation Failed");
        errorResponse.setDetails("Input validation error");
        errorResponse.setErrors(errors);
        errorResponse.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Validation Failed");
        errorResponse.setDetails("Constraint violation");
        errorResponse.setErrors(errors);
        errorResponse.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        List<String> errors = new ArrayList<>();
        errors.add(ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Server Error");
        errorResponse.setDetails(ex.getMessage());
        errorResponse.setErrors(errors);
        errorResponse.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}