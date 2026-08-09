package com.sjtb.reporting.exception;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException e) { return response(e.getStatus(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst().map(x -> x.getField() + " " + x.getDefaultMessage()).orElse("Request validation failed");
        return response(HttpStatus.BAD_REQUEST, message);
    }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<Map<String, Object>> denied() { return response(HttpStatus.FORBIDDEN, "Permission denied"); }
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<Map<String, Object>> auth() { return response(HttpStatus.UNAUTHORIZED, "Invalid username or password"); }
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String, Object>> other(Exception e) {
        log.error("Unhandled API exception", e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", LocalDateTime.now().toString(), "status", status.value(), "message", message));
    }
}
