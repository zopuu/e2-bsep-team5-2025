package com.besp.pki.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private Map<String, Object> baseBody(String code, String msg, HttpServletRequest req) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "path", req.getRequestURI(),
                "error", code,
                "message", msg
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(baseBody("VALIDATION", ex.getMessage(), req));
    }

    // JSON nečitljiv/loš format
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(baseBody("BAD_JSON", "Malformed JSON body", req));
    }

    // @Valid na @RequestBody – saberi field greške
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "path", req.getRequestURI(),
                "error", "VALIDATION",
                "message", "Validation failed",
                "fieldErrors", fields
        ));
    }

    // @Valid na query/path parametrima
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleParamValidation(ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, String>> fields = ex.getConstraintViolations().stream()
                .map(cv -> Map.of("param", cv.getPropertyPath().toString(), "message", cv.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now().toString(),
                "path", req.getRequestURI(),
                "error", "VALIDATION",
                "message", "Validation failed",
                "paramErrors", fields
        ));
    }

    // npr. Spring Security
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(403).body(baseBody("FORBIDDEN", "Access denied", req));
    }

    // Fallback (uvek poslednji)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(500).body(baseBody("INTERNAL", "Internal server error", req));
    }
}
