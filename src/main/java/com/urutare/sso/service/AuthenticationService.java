package com.urutare.sso.service;

import com.urutare.sso.dto.AuthResponse;
import com.urutare.sso.dto.LoginRequest;
import com.urutare.sso.dto.RegisterRequest;
import com.urutare.sso.entity.RefreshToken;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.AuthProvider;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.RefreshTokenRepository;
import com.urutare.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        UserAccount user = UserAccount.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .roles(Set.of(Role.ROLE_USER))
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        // Send verification email
        emailVerificationService.sendVerificationEmail(user);

        // Generate tokens (user can use the app but with limited access until verified)
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(RegisterRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );

            UserAccount user = (UserAccount) authentication.getPrincipal();
            
            // Revoke existing refresh tokens for security
            refreshTokenRepository.revokeAllByUser(user);
            
            log.info("User logged in: {}", user.getEmail());
            return generateAuthResponse(user);

        } catch (DisabledException e) {
            throw new RuntimeException("Account not verified. Please check your email for verification link.");
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid email or password");
        } catch (AuthenticationException e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenValue) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue);
        
        if (refreshTokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        UserAccount user = refreshToken.getUser();
        
        // Revoke the old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        
        log.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue);
        
        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("User logged out: {}", refreshToken.getUser().getEmail());
        }
    }

    @Transactional
    public void logoutAllDevices(String userEmail) {
        Optional<UserAccount> userOpt = userRepository.findByEmailIgnoreCase(userEmail);
        if (userOpt.isPresent()) {
            refreshTokenRepository.revokeAllByUser(userOpt.get());
            log.info("All devices logged out for user: {}", userEmail);
        }
    }

    public AuthResponse generateAuthResponse(UserAccount user) {
        try {
            // Generate access token
            log.info("Generating auth response for user: {}", user.getEmail());
            String accessToken = jwtService.generateAccessToken(user);
            
            // Generate refresh token
            String refreshTokenValue = jwtService.generateRefreshToken(user);
            
            // Save refresh token to database
            RefreshToken refreshToken = RefreshToken.builder()
                    .token(refreshTokenValue)
                    .user(user)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)) // 7 days
                    .build();
            
            refreshTokenRepository.save(refreshToken);

            // Build user info
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getName())
                    .roles(user.getRoles())
                    .emailVerified(user.isEmailVerified())
                    .provider(user.getProvider().name())
                    .build();

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenValue)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration() / 1000) // Convert to seconds
                    .user(userInfo)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate auth response for user: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate authentication tokens", e);
        }
    }

    public UserAccount getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean isEmailTaken(String email) {
        return userRepository.findByEmailIgnoreCase(email).isPresent();
    }
}
