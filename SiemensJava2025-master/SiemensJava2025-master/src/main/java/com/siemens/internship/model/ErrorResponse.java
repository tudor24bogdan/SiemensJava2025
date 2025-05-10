package com.siemens.internship.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for standardized error responses
 *  * Used by the GlobalExceptionHandler to provide consistent error information.
 *
 *  the new class improves the code, create standardized error response
 *  includes timestamp for logging and debugging process
 *  a place to collect multiple validation errors
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private String details;
    private List<String> errors;
    private LocalDateTime timestamp = LocalDateTime.now();
}