package com.taskmanagement.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.auth.dto.LoginRequest;
import com.taskmanagement.auth.dto.RegisterRequest;
import com.taskmanagement.auth.model.User;
import com.taskmanagement.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // Clean up database before each test
        userRepository.deleteAll();
    }

    @Test
    void fullAuthenticationFlow_Success() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        
        String registerResponse = mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify user was created in database
        User createdUser = userRepository.findByEmail("test@example.com").orElse(null);
        assert createdUser != null;
        assert createdUser.isActive();

        // 2. Login with the same credentials
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void register_DuplicateEmail_ShouldFail() throws Exception {
        // 1. Register first user
        RegisterRequest firstRequest = new RegisterRequest("First User", "test@example.com", "password123");
        
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // 2. Try to register second user with same email
        RegisterRequest secondRequest = new RegisterRequest("Second User", "test@example.com", "password456");
        
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with email test@example.com already exists"));
    }

    @Test
    void login_InvalidCredentials_ShouldFail() throws Exception {
        // 1. Register a user
        RegisterRequest registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");
        
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_NonexistentUser_ShouldFail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");
        
        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_ShouldFail() throws Exception {
        mockMvc.perform(get("/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test-user-id")
    void accessProtectedEndpoint_WithValidToken_ShouldSucceed() throws Exception {
        // First create a user in the database
        User testUser = new User("test@example.com", "Test User", "hashedPassword");
        testUser.setUserId("test-user-id");
        userRepository.save(testUser);

        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("test-user-id"));
    }
}