package de.shopeasy.returnsmanagement.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResponseStatusException ex) {

        int status = ex.getStatusCode().value();

        String message = ex.getReason() != null
                ? ex.getReason()
                : "Die angeforderte Ressource wurde nicht gefunden.";

        Map<String, Object> response = Map.of(
                "status", status,
                "error", status == 404
                        ? "Ressource nicht gefunden"
                        : "Anfrage fehlgeschlagen",
                "message", message
        );

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(response);
    }
}
