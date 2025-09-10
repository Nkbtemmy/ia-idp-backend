package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.dto.AuthResponse;
import com.urutare.sso.dto.LoginRequest;
import com.urutare.sso.dto.RefreshTokenRequest;
import com.urutare.sso.dto.RegisterRequest;
import com.urutare.sso.service.AuthenticationService;
import com.urutare.sso.service.EmailVerificationService;
import com.urutare.sso.service.JwtService;
import com.urutare.sso.service.SeedDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sso-service/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and token management endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;

    @Operation(summary = "User Registration", description = "Register a new user with email and password")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authenticationService.register(request);
            return ResponseEntity.ok(ApiResponse.ok("Registration successful. Please check your email for verification.", response));
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "User Login", description = "Authenticate user with email and password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authenticationService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "Refresh Access Token", description = "Get a new access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authenticationService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "Logout", description = "Logout user and revoke refresh token")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authenticationService.logout(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "Logout All Devices", description = "Logout user from all devices")
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtService.getEmailFromToken(token);
            authenticationService.logoutAllDevices(email);
            return ResponseEntity.ok(ApiResponse.ok("Logged out from all devices successfully", null));
        } catch (Exception e) {
            log.error("Logout all failed", e);
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "Verify Email", description = "Verify user email with token")
    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam String token, 
                           @RequestParam(required = false) String redirect, 
                           HttpServletResponse response) throws Exception {
        try {
            boolean verified = emailVerificationService.verifyEmail(token);
            if (verified) {
                if (redirect != null && !redirect.isBlank()) {
                    String sep = redirect.contains("?") ? "&" : "?";
                    response.sendRedirect(redirect + sep + "verified=true");
                } else {
                    response.sendRedirect("/verified.html?status=success");
                }
            } else {
                if (redirect != null && !redirect.isBlank()) {
                    String sep = redirect.contains("?") ? "&" : "?";
                    response.sendRedirect(redirect + sep + "verified=false&error=invalid_token");
                } else {
                    response.sendRedirect("/verified.html?status=error&message=invalid_token");
                }
            }
        } catch (Exception e) {
            log.error("Email verification failed for token: {}", token, e);
            if (redirect != null && !redirect.isBlank()) {
                String sep = redirect.contains("?") ? "&" : "?";
                response.sendRedirect(redirect + sep + "verified=false&error=verification_failed");
            } else {
                response.sendRedirect("/verified.html?status=error&message=verification_failed");
            }
        }
    }

    @Operation(summary = "Resend Verification Email", description = "Resend email verification link")
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestParam String email) {
        try {
            emailVerificationService.resendVerificationEmail(email);
            return ResponseEntity.ok(ApiResponse.ok("Verification email sent successfully", null));
        } catch (Exception e) {
            log.error("Resend verification failed for email: {}", email, e);
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @Operation(summary = "Validate Token", description = "Validate access token and return claims")
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            var decodedJWT = jwtService.validateToken(token);
            
            Map<String, Object> claims = Map.of(
                "valid", true,
                "subject", decodedJWT.getSubject(),
                "userId", decodedJWT.getClaim("userId").asString(),
                "email", decodedJWT.getClaim("email").asString(),
                "name", decodedJWT.getClaim("name") != null ? decodedJWT.getClaim("name").asString() : "",
                "roles", decodedJWT.getClaim("roles").asList(String.class),
                "provider", decodedJWT.getClaim("provider").asString(),
                "emailVerified", decodedJWT.getClaim("emailVerified").asBoolean(),
                "exp", decodedJWT.getExpiresAt().getTime() / 1000,
                "iat", decodedJWT.getIssuedAt().getTime() / 1000
            );
            
            return ResponseEntity.ok(claims);
        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Check Email Availability", description = "Check if email is already registered")
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            boolean taken = authenticationService.isEmailTaken(email);
            return ResponseEntity.ok(Map.of("available", !taken, "email", email));
        } catch (Exception e) {
            log.error("Email check failed for: {}", email, e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Failed to check email availability"));
        }
    }

    @RestController
    @RequestMapping("/api/v1/sso-service/admin/seed")
    @RequiredArgsConstructor
    public static class SeedDataController {

        private final SeedDataService seedDataService;

        @PostMapping("/create")
        public ResponseEntity<Map<String, String>> createSeedData() {
            try {
                seedDataService.createDevelopmentData();
                seedDataService.printSeedDataSummary();
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Seed data created successfully"
                ));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to create seed data: " + e.getMessage()
                ));
            }
        }

        @DeleteMapping("/clear")
        public ResponseEntity<Map<String, String>> clearAllData() {
            try {
                seedDataService.clearAllData();
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All data cleared successfully"
                ));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to clear data: " + e.getMessage()
                ));
            }
        }

        @GetMapping("/summary")
        public ResponseEntity<Map<String, String>> getSeedDataSummary() {
            try {
                seedDataService.printSeedDataSummary();
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Check logs for seed data summary"
                ));
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to get summary: " + e.getMessage()
                ));
            }
        }
    }
}
