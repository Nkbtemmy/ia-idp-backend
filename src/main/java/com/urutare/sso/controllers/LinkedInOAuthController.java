package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.dto.AuthResponse;
import com.urutare.sso.service.LinkedInOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sso-service/auth/linkedin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LinkedIn OAuth", description = "LinkedIn OAuth authentication endpoints")
public class LinkedInOAuthController {

    private final LinkedInOAuthService linkedInOAuthService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Operation(summary = "LinkedIn OAuth Login", description = "Initiate LinkedIn OAuth flow")
    @GetMapping("/login")
    public void linkedInLogin(@RequestParam(required = false) String redirect,
                             HttpServletResponse response) throws Exception {
        try {
            String state = UUID.randomUUID().toString();
            // In production, you should store the state and redirect URL in session or cache
            // For simplicity, we'll include redirect in state (encode it properly in production)
            String stateWithRedirect = state + (redirect != null ? "|" + redirect : "");
            
            String authUrl = linkedInOAuthService.getAuthorizationUrl(stateWithRedirect);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("LinkedIn login initiation failed", e);
            response.sendRedirect(frontendUrl + "/login?error=linkedin_init_failed");
        }
    }

    @Operation(summary = "LinkedIn OAuth Callback", description = "Handle LinkedIn OAuth callback")
    @GetMapping("/callback")
    public void linkedInCallback(@RequestParam String code,
                                @RequestParam String state,
                                @RequestParam(required = false) String error,
                                HttpServletResponse response) throws Exception {
        try {
            if (error != null) {
                log.error("LinkedIn OAuth error: {}", error);
                response.sendRedirect(frontendUrl + "/login?error=linkedin_" + error);
                return;
            }

            // Extract redirect URL from state (in production, retrieve from session/cache)
            String[] stateParts = state.split("\\|");
            String originalState = stateParts[0];
            String redirectUrl = stateParts.length > 1 ? stateParts[1] : null;

            AuthResponse authResponse = linkedInOAuthService.handleCallback(code, originalState);
            
            // Redirect to frontend with tokens (in production, use secure cookies or session)
            String targetUrl = redirectUrl != null ? redirectUrl : (frontendUrl + "/dashboard");
            String separator = targetUrl.contains("?") ? "&" : "?";
            
            response.sendRedirect(targetUrl + separator + 
                "access_token=" + authResponse.getAccessToken() + 
                "&refresh_token=" + authResponse.getRefreshToken() +
                "&token_type=" + authResponse.getTokenType() +
                "&expires_in=" + authResponse.getExpiresIn());
                
        } catch (Exception e) {
            log.error("LinkedIn OAuth callback failed", e);
            response.sendRedirect(frontendUrl + "/login?error=linkedin_callback_failed");
        }
    }

    @Operation(summary = "LinkedIn OAuth Status", description = "Get LinkedIn OAuth configuration status")
    @GetMapping("/status")
    public ResponseEntity<?> getLinkedInStatus() {
        try {
            // Return configuration status without sensitive information
            Map<String, Object> status = Map.of(
                "enabled", true,
                "provider", "linkedin",
                "authUrl", "/api/v1/sso-service/auth/linkedin/login"
            );
            return ResponseEntity.ok(ApiResponse.ok("LinkedIn OAuth status", status));
        } catch (Exception e) {
            log.error("Failed to get LinkedIn status", e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("Failed to get LinkedIn status"));
        }
    }
}
