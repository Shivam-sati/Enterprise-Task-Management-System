package com.taskmanagement.auth.service;

import com.taskmanagement.auth.model.AuthProvider;
import com.taskmanagement.auth.model.Role;
import com.taskmanagement.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Set test values using reflection
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "myTestSecretKeyForJWTTokenGenerationAndValidationThatShouldBeAtLeast256BitsLong");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days

        testUser = new User();
        testUser.setUserId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(Role.USER);
        testUser.setProvider(AuthProvider.LOCAL);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void generateAccessToken_Success() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
        
        // Verify token contents
        assertEquals(testUser.getEmail(), jwtService.getUserEmailFromToken(token));
        assertEquals(testUser.getUserId(), jwtService.getUserIdFromToken(token));
        assertEquals(testUser.getRole().getValue(), jwtService.getRoleFromToken(token));
        assertEquals("access", jwtService.getTokenType(token));
    }

    @Test
    void generateRefreshToken_Success() {
        // When
        String token = jwtService.generateRefreshToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
        assertTrue(jwtService.isRefreshToken(token));
        
        // Verify token contents
        assertEquals(testUser.getEmail(), jwtService.getUserEmailFromToken(token));
        assertEquals(testUser.getUserId(), jwtService.getUserIdFromToken(token));
        assertEquals("refresh", jwtService.getTokenType(token));
    }

    @Test
    void validateToken_ValidToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isValid = jwtService.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_NullToken() {
        // When
        boolean isValid = jwtService.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_EmptyToken() {
        // When
        boolean isValid = jwtService.validateToken("");

        // Then
        assertFalse(isValid);
    }

    @Test
    void getUserEmailFromToken_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String email = jwtService.getUserEmailFromToken(token);

        // Then
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    void getUserIdFromToken_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String userId = jwtService.getUserIdFromToken(token);

        // Then
        assertEquals(testUser.getUserId(), userId);
    }

    @Test
    void getRoleFromToken_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String role = jwtService.getRoleFromToken(token);

        // Then
        assertEquals(testUser.getRole().getValue(), role);
    }

    @Test
    void isRefreshToken_AccessToken() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);

        // When
        boolean isRefreshToken = jwtService.isRefreshToken(accessToken);

        // Then
        assertFalse(isRefreshToken);
    }

    @Test
    void isRefreshToken_RefreshToken() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When
        boolean isRefreshToken = jwtService.isRefreshToken(refreshToken);

        // Then
        assertTrue(isRefreshToken);
    }

    @Test
    void isTokenExpired_ValidToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void getExpirationDateFromToken_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        var expirationDate = jwtService.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.getTime() > System.currentTimeMillis());
    }

    @Test
    void getJwtExpiration_Success() {
        // When
        Long expiration = jwtService.getJwtExpiration();

        // Then
        assertEquals(86400000L, expiration);
    }
}