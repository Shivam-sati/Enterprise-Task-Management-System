package com.taskmanagement.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.auth.dto.*;
import com.taskmanagement.auth.model.Role;
import com.taskmanagement.auth.model.User;
import com.taskmanagement.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");
        
        testUser = new User();
        testUser.setUserId("test-user-id");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(Role.USER);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                testUser.getUserId(),
                testUser.getName(),
                testUser.getEmail(),
                testUser.getRole()
        );

        authResponse = new AuthResponse(
                "access-token",
                "refresh-token",
                86400000L,
                userInfo
        );
    }

    @Test
    void register_Success() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.user.email").value(testUser.getEmail()));
    }

    @Test
    void register_ValidationError() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email", "123");

        // When & Then
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_Success() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.userId").value(testUser.getUserId()));
    }

    @Test
    void login_ValidationError() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest("", "");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_Success() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("valid-refresh-token");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void logout_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/auth/logout")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void getProfile_Success() throws Exception {
        // Given
        when(authService.getUserProfile("test-user-id")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.name").value(testUser.getName()));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void updateProfile_Success() throws Exception {
        // Given
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Updated Name", null);
        testUser.setName("Updated Name");
        when(authService.updateUserProfile(eq("test-user-id"), any(UpdateProfileRequest.class)))
                .thenReturn(testUser);

        // When & Then
        mockMvc.perform(put("/auth/profile")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void validateToken_Success() throws Exception {
        // Given
        when(authService.getUserProfile("test-user-id")).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(testUser.getUserId()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.role").value(testUser.getRole().getValue()));
    }
}