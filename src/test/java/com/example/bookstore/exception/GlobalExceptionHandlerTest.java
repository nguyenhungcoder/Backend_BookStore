package com.example.bookstore.exception;

import com.example.bookstore.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthenticationCredentialsNotFound_shouldReturnUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/publishers");

        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationCredentialsNotFound(
                new AuthenticationCredentialsNotFoundException("Authentication required"),
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication required", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturnForbidden() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/publishers");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Admin role required"),
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Admin role required", response.getBody().getMessage());
    }
}
