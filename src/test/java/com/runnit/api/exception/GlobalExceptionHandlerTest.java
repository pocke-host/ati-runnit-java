package com.runnit.api.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleNotFound(new ResourceNotFoundException("Activity not found"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Activity not found", response.getBody().get("error"));
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleBadRequest(new BadRequestException("Invalid input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().get("error"));
    }

    @Test
    void handleUnauthorized_returns401() {
        ResponseEntity<Map<String, String>> response =
                handler.handleUnauthorized(new UnauthorizedException("Invalid credentials"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().get("error"));
    }

    @Test
    void handleForbidden_returns403() {
        ResponseEntity<Map<String, String>> response =
                handler.handleForbidden(new ForbiddenException("Access denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
    }

    @Test
    void handleConflict_returns409() {
        ResponseEntity<Map<String, String>> response =
                handler.handleConflict(new ConflictException("Email already exists"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().get("error"));
    }

    @Test
    void handleValidation_returns400WithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("error"));
        assertTrue(((List<?>) response.getBody().get("details")).contains("email: must not be blank"));
    }

    @Test
    void handleGeneric_returns500WithoutLeakingDetails() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGeneric(new RuntimeException("some internal DB error details"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("error"));
        assertFalse(response.getBody().get("error").contains("DB error"));
    }
}
