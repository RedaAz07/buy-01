package com.Media.exceptions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice // Tells Spring: "Send all crashes here!"
public class GlobalExceptionHandler {

    // ────────────────────────────────────────────────────────
    // 1. 400 BAD REQUEST (Validation Errors from DTOs)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("message", "Invalid data provided");
        body.put("fieldErrors", validationErrors); // Attach the specific field errors here

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Object> handleMultipartException(MultipartException ex) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid multipart request. Please provide a valid file.");
    }

    // ────────────────────────────────────────────────────────
    // 2. 404 NOT FOUND (When a DB search comes up empty)
    // ────────────────────────────────────────────────────────
    // @ExceptionHandler(EntityNotFoundException.class)
    // public ResponseEntity<Object> handleNotFound(EntityNotFoundException ex) {
    // return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    // }

    // ────────────────────────────────────────────────────────
    // 3. 409 CONFLICT (Duplicate usernames, emails, etc.)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataConflict(DataIntegrityViolationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Database conflict: Duplicate entry or foreign key violation.");
    }

    // ────────────────────────────────────────────────────────
    // 4. 401 UNAUTHORIZED (Wrong password / Bad JWT)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
    }

    // ────────────────────────────────────────────────────────
    // 5. 403 FORBIDDEN (Logged in, but not allowed to do this)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    // ────────────────────────────────────────────────────────
    // 5. 423 FORBIDDEN (Logged in, but not allowed to do this)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Object> handleBanned(LockedException ex) {
        return buildErrorResponse(HttpStatus.LOCKED, "you are banned");
    }
    // Method 2 (You probably have something like this right next to it)

    // ────────────────────────────────────────────────────────
    // 6. 400 BAD REQUEST (For your custom RuntimeExceptions)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // @ExceptionHandler(ApiException.class)
    // public ResponseEntity<Object> handleApiException(ApiException ex) {
    //     return buildErrorResponse(ex.getStatus(), ex.getMessage());
    // }

    // @ExceptionHandler(MaxUploadSizeExceededException.class)
    // public ResponseEntity<Object> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
    //     return buildErrorResponse(HttpStatus.BAD_REQUEST,
    //             "File is too large. Maximum allowed size is 2 MB.");
    // }

    // @ExceptionHandler(NoResourceFoundException.class)
    // public ResponseEntity<Object> handleEndpointNotFound(NoResourceFoundException ex) {
    //     return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested endpoint does not exist.");
    // }

    // ────────────────────────────────────────────────────────
    // 7. 500 INTERNAL SERVER ERROR (The Catch-All for unknown bugs)
    // ────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllOtherExceptions(Exception ex) {

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred on the server.");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Object> handleDisabledAccount(DisabledException ex) {
        return buildErrorResponse(HttpStatus.LOCKED,
                "Your account has been banned or disabled. Please contact support.");
    }

    private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return new ResponseEntity<>(body, status);
    }
}
