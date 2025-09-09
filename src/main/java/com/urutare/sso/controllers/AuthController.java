package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.dto.LoginRequest;
import com.urutare.sso.dto.RefreshTokenRequest;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.service.ClientAppService;
import com.urutare.sso.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sso-service/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ClientAppService clientAppService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Validate client credentials
            if (!clientAppService.validateClient(request.getClientId(), request.getClientSecret())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid client credentials"));
            }

            // Find and validate user
            UserAccount user = userRepository.findByEmailIgnoreCase(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid credentials"));
            }

            if (!user.isEmailVerified()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email not verified"));
            }

            // Generate token pair
            Map<String, Object> tokens = tokenService.generateTokenPair(user, request.getClientId());
            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            // Validate client credentials
            if (!clientAppService.validateClient(request.getClientId(), request.getClientSecret())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid client credentials"));
            }

            Map<String, Object> tokens = tokenService.refreshAccessToken(
                    request.getRefreshToken(), 
                    request.getClientId()
            );
            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Token refresh failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        try {
            tokenService.revokeRefreshToken(request.getRefreshToken());
            return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Logout failed: " + e.getMessage()));
        }
    }

    @GetMapping("/jwks")
    public ResponseEntity<?> getJWKS() {
        try {
            Map<String, Object> jwks = tokenService.getPublicKeyJWKS();
            return ResponseEntity.ok(jwks);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to get JWKS: " + e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var decodedJWT = tokenService.verifyAccessToken(token);
            
            Map<String, Object> claims = Map.of(
                "valid", true,
                "subject", decodedJWT.getSubject(),
                "userId", decodedJWT.getClaim("userId").asString(),
                "email", decodedJWT.getClaim("email").asString(),
                "roles", decodedJWT.getClaim("roles").asList(String.class),
                "exp", decodedJWT.getExpiresAt().getTime() / 1000
            );
            
            return ResponseEntity.ok(claims);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
