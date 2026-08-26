package com.vanai.backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        int statusCode = exception.getStatusCode().value();

        HttpStatus status = HttpStatus.resolve(statusCode);
        String error = status != null
                ? status.getReasonPhrase()
                : "Request Error";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", statusCode);
        body.put("error", error);
        body.put("message", exception.getReason());

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(body);
    }
}
