package com.taskmanagement.auth.service;

import com.taskmanagement.auth.dto.*;
import com.taskmanagement.auth.exception.AuthenticationException;
import com.taskmanagement.auth.exception.UserAlreadyExistsException;
import com.taskmanagement.auth.exception.UserNotFoundException;
import com.taskmanagement.auth.metrics.AuthMetrics;
import com.taskmanagement.auth.model.AuthProvider;
import com.taskmanagement.auth.model.Role;
import com.taskmanagement.auth.model.User;
import com.taskmanagement.auth.repository.UserRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMetrics authMetrics;
    
    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                      JwtService jwtService, AuthMetrics authMetrics) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authMetrics = authMetrics;
    }
    
    public AuthResponse register(RegisterRequest request) {
        logger.info("Attempting to register user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }
        
        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        
        // Save user
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", savedUser.getUserId());
        
        // Record metrics
        authMetrics.incrementRegistrations();
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);
        
        return createAuthResponse(accessToken, refreshToken, savedUser);
    }
    
    public AuthResponse login(LoginRequest request) {
        logger.info("Attempting to login user with email: {}", request.getEmail());
        
        Timer.Sample loginTimer = authMetrics.startLoginTimer();
        authMetrics.incrementLoginAttempts();
        
        try {
            // Find user by email
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        authMetrics.incrementLoginFailures();
                        return new AuthenticationException("Invalid email or password");
                    });
            
            // Check if user is active
            if (!user.isActive()) {
                authMetrics.incrementLoginFailures();
                throw new AuthenticationException("User account is deactivated");
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                authMetrics.incrementLoginFailures();
                throw new AuthenticationException("Invalid email or password");
            }
            
            // Update last login
            user.updateLastLogin();
            userRepository.save(user);
            
            logger.info("User logged in successfully: {}", user.getUserId());
            
            // Record successful login
            authMetrics.incrementLoginSuccesses();
            authMetrics.recordLoginDuration(loginTimer);
            
            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            return createAuthResponse(accessToken, refreshToken, user);
        } catch (Exception e) {
            authMetrics.recordLoginDuration(loginTimer);
            throw e;
        }
    }
    
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Attempting to refresh token");
        
        String refreshToken = request.getRefreshToken();
        
        // Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }
        
        // Check if it's actually a refresh token
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new AuthenticationException("Token is not a refresh token");
        }
        
        // Get user from token
        String userEmail = jwtService.getUserEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Check if user is still active
        if (!user.isActive()) {
            throw new AuthenticationException("User account is deactivated");
        }
        
        logger.info("Token refreshed successfully for user: {}", user.getUserId());
        
        // Record metrics
        authMetrics.incrementTokenRefreshes();
        
        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        return createAuthResponse(newAccessToken, newRefreshToken, user);
    }
    
    public User getUserProfile(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    public User updateUserProfile(String userId, UpdateProfileRequest request) {
        User user = getUserProfile(userId);
        
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        
        if (request.getPreferences() != null) {
            user.setPreferences(request.getPreferences());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    public User processOAuthUser(String email, String name, AuthProvider provider, String providerId) {
        logger.info("Processing OAuth user: {} from provider: {}", email, provider);
        
        // Check if user exists with this email
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update provider info if it's a local account
            if (user.getProvider() == AuthProvider.LOCAL) {
                user.setProvider(provider);
                user.setProviderId(providerId);
            }
            user.updateLastLogin();
            return userRepository.save(user);
        }
        
        // Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setRole(Role.USER);
        newUser.setActive(true);
        
        return userRepository.save(newUser);
    }
    
    private AuthResponse createAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
        
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getJwtExpiration(),
                userInfo
        );
    }
}