package com.taskmanagement.auth.oauth2;

import com.taskmanagement.auth.model.AuthProvider;
import com.taskmanagement.auth.model.User;
import com.taskmanagement.auth.service.AuthService;
import com.taskmanagement.auth.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    
    private final AuthService authService;
    private final JwtService jwtService;
    
    @Autowired
    public OAuth2AuthenticationSuccessHandler(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        try {
            // Extract user information from OAuth2 provider
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String providerId = oAuth2User.getAttribute("sub"); // Google's user ID
            
            if (email == null) {
                logger.error("Email not found in OAuth2 user attributes");
                handleAuthenticationFailure(response, "Email not provided by OAuth2 provider");
                return;
            }
            
            // Process OAuth user (create or update)
            User user = authService.processOAuthUser(email, name, AuthProvider.GOOGLE, providerId);
            
            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // Redirect to frontend with tokens
            String targetUrl = buildTargetUrl(accessToken, refreshToken);
            
            logger.info("OAuth2 authentication successful for user: {}", user.getUserId());
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            
        } catch (Exception e) {
            logger.error("OAuth2 authentication failed", e);
            handleAuthenticationFailure(response, "Authentication failed: " + e.getMessage());
        }
    }
    
    private String buildTargetUrl(String accessToken, String refreshToken) {
        return UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
                .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .build().toUriString();
    }
    
    private void handleAuthenticationFailure(HttpServletResponse response, String errorMessage) throws IOException {
        String errorUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/error")
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build().toUriString();
        
        response.sendRedirect(errorUrl);
    }
}