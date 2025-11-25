package com.taskmanagement.auth.service;

import com.taskmanagement.auth.dto.LoginRequest;
import com.taskmanagement.auth.dto.RegisterRequest;
import com.taskmanagement.auth.dto.AuthResponse;
import com.taskmanagement.auth.dto.RefreshTokenRequest;
import com.taskmanagement.auth.exception.AuthenticationException;
import com.taskmanagement.auth.exception.UserAlreadyExistsException;
import com.taskmanagement.auth.exception.UserNotFoundException;
import com.taskmanagement.auth.model.AuthProvider;
import com.taskmanagement.auth.model.Role;
import com.taskmanagement.auth.model.User;
import com.taskmanagement.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRole(Role.USER);
        testUser.setProvider(AuthProvider.LOCAL);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");
    }

    @Test
    void register_Success() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals(testUser.getUserId(), response.getUser().getUserId());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_UserAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getUser());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void login_UserNotFound() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
        
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_InvalidPassword() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
        
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_InactiveUser() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.login(loginRequest));
        
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void refreshToken_Success() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("valid-refresh-token");
        when(jwtService.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.isRefreshToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromToken(refreshRequest.getRefreshToken())).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);

        // When
        AuthResponse response = authService.refreshToken(refreshRequest);

        // Then
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());

        verify(jwtService).validateToken(refreshRequest.getRefreshToken());
        verify(jwtService).isRefreshToken(refreshRequest.getRefreshToken());
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void refreshToken_InvalidToken() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalid-token");
        when(jwtService.validateToken(refreshRequest.getRefreshToken())).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshRequest));
        
        verify(jwtService).validateToken(refreshRequest.getRefreshToken());
        verify(jwtService, never()).isRefreshToken(anyString());
    }

    @Test
    void refreshToken_NotRefreshToken() {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("access-token");
        when(jwtService.validateToken(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.isRefreshToken(refreshRequest.getRefreshToken())).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> authService.refreshToken(refreshRequest));
        
        verify(jwtService).validateToken(refreshRequest.getRefreshToken());
        verify(jwtService).isRefreshToken(refreshRequest.getRefreshToken());
    }

    @Test
    void getUserProfile_Success() {
        // Given
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.of(testUser));

        // When
        User result = authService.getUserProfile(testUser.getUserId());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUserId(), result.getUserId());
        assertEquals(testUser.getEmail(), result.getEmail());

        verify(userRepository).findByUserId(testUser.getUserId());
    }

    @Test
    void getUserProfile_UserNotFound() {
        // Given
        when(userRepository.findByUserId(testUser.getUserId())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> authService.getUserProfile(testUser.getUserId()));
        
        verify(userRepository).findByUserId(testUser.getUserId());
    }

    @Test
    void processOAuthUser_NewUser() {
        // Given
        String email = "oauth@example.com";
        String name = "OAuth User";
        String providerId = "google-123";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = authService.processOAuthUser(email, name, AuthProvider.GOOGLE, providerId);

        // Then
        assertNotNull(result);
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void processOAuthUser_ExistingUser() {
        // Given
        String email = testUser.getEmail();
        String name = "OAuth User";
        String providerId = "google-123";
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = authService.processOAuthUser(email, name, AuthProvider.GOOGLE, providerId);

        // Then
        assertNotNull(result);
        assertEquals(AuthProvider.GOOGLE, result.getProvider());
        assertEquals(providerId, result.getProviderId());
        
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(testUser);
    }
}