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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
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

    @Value("${spring.security.oauth2.client.registration.linkedin.redirect-uri:http://localhost:8080/api/v1/sso-service/auth/linkedin/callback}")
    private String redirectUrl;

    @Value("${app.frontend.login-success-path:/callback-widget.html}")
    private String loginSuccessPath;

    @Value("${app.frontend.login-error-path:/error}")
    private String loginErrorPath;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000}")
    private String allowedOrigins;


    @Operation(summary = "LinkedIn OAuth Login", description = "Initiate LinkedIn OAuth flow")
    @GetMapping("/login")
    public void linkedInLogin(HttpServletResponse response) throws Exception {
        try {
            String state = UUID.randomUUID().toString();
            String stateWithRedirect = state + (redirectUrl != null ? "|" + redirectUrl : "");
            
            String authUrl = linkedInOAuthService.getAuthorizationUrl(stateWithRedirect);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            log.error("LinkedIn login initiation failed", e);
            response.sendRedirect(frontendUrl + "/login?error=linkedin_init_failed");
        }
    }

    @Operation(summary = "LinkedIn OAuth Callback", description = "Handle LinkedIn OAuth callback")
    @GetMapping("/callback")
    public void linkedInCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletResponse response) throws IOException {

        try {
            log.info("LinkedIn OAuth callback initiated - State: {}, Has Code: {}, Error: {}",
                    state, code != null, error);

            // Handle OAuth errors first
            if (error != null) {
                log.error("LinkedIn OAuth error: {} - {}", error, error_description);
                handleOAuthError(error, error_description, response);
                return;
            }

            // Validate required parameters
            if (code == null || code.trim().isEmpty()) {
                log.error("Missing authorization code in LinkedIn callback");
                redirectWithError(response, "missing_authorization_code", "Authorization code is required");
                return;
            }

            if (state == null || state.trim().isEmpty()) {
                log.error("Missing state parameter in LinkedIn callback");
                redirectWithError(response, "missing_state", "State parameter is required");
                return;
            }

            // Parse state parameter safely
            StateInfo stateInfo = parseStateParameter(state);

            // Process LinkedIn authentication
            AuthResponse authResponse = linkedInOAuthService.handleCallback(code, stateInfo.originalState());

            if (authResponse == null) {
                log.error("Authentication service returned null response");
                redirectWithError(response, "authentication_failed", "Authentication failed");
                return;
            }

            log.info("LinkedIn authentication successful for user: {}",
                    authResponse.getUser().getEmail() != null ? authResponse.getUser().getEmail() : "unknown");
            String frontendRedirect =  frontendUrl + loginSuccessPath;
            // Redirect to frontend with tokens
            redirectWithSuccess(response, authResponse, frontendRedirect);

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters in LinkedIn callback: {}", e.getMessage());
            redirectWithError(response, "invalid_parameters", e.getMessage());

        } catch (SecurityException e) {
            log.error("Security violation in LinkedIn callback: {}", e.getMessage());
            redirectWithError(response, "security_error", "Authentication failed for security reasons");

        } catch (Exception e) {
            log.error("Unexpected error in LinkedIn OAuth callback", e);
            redirectWithError(response, "callback_failed", "Authentication failed due to unexpected error");
        }
    }

    private void handleOAuthError(String error, String errorDescription, HttpServletResponse response)
            throws IOException {

        String userFriendlyMessage;
        String errorCode;

        switch (error) {
            case "access_denied":
                userFriendlyMessage = "Access was denied. Please try again and grant the necessary permissions.";
                errorCode = "access_denied";
                break;
            case "invalid_request":
                userFriendlyMessage = "Invalid request. Please try logging in again.";
                errorCode = "invalid_request";
                break;
            case "unauthorized_client":
                userFriendlyMessage = "Application not authorized. Please contact support.";
                errorCode = "unauthorized_client";
                break;
            case "unsupported_response_type":
                userFriendlyMessage = "Configuration error. Please contact support.";
                errorCode = "config_error";
                break;
            case "invalid_scope":
                userFriendlyMessage = "Invalid permissions requested. Please contact support.";
                errorCode = "invalid_scope";
                break;
            case "server_error":
                userFriendlyMessage = "LinkedIn server error. Please try again later.";
                errorCode = "server_error";
                break;
            case "temporarily_unavailable":
                userFriendlyMessage = "Service temporarily unavailable. Please try again later.";
                errorCode = "temporarily_unavailable";
                break;
            default:
                userFriendlyMessage = "Authentication failed. Please try again.";
                errorCode = "unknown_error";
        }

        redirectWithError(response, errorCode, userFriendlyMessage);
    }

    private StateInfo parseStateParameter(String state) {
        try {
            // Expected format: "originalState|redirectUrl" or just "originalState"
            String[] stateParts = state.split("\\|", 2); // Limit to 2 parts
            String originalState = stateParts[0];
            String redirectUrl = stateParts.length > 1 ? stateParts[1] : null;

            // Validate original state (implement your state validation logic)
            validateStateToken(originalState);

            // Sanitize redirect URL
            String sanitizedRedirectUrl = sanitizeRedirectUrl(redirectUrl);

            return new StateInfo(originalState, sanitizedRedirectUrl);

        } catch (Exception e) {
            log.error("Failed to parse state parameter: {}", state, e);
            throw new SecurityException("Invalid state parameter");
        }
    }

    private void validateStateToken(String stateToken) {
        // Implement your state validation logic here
        // This should verify that the state token is valid and not expired
        // For example, check against a cache/database of issued state tokens

        if (stateToken == null || stateToken.length() < 10) {
            throw new SecurityException("Invalid state token");
        }

        // TODO: Implement actual state validation
        // - Check if state exists in cache/database
        // - Check if state is not expired
        // - Mark state as used to prevent replay attacks

        log.debug("State token validation passed for: {}", stateToken);
    }

    private String sanitizeRedirectUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            return null;
        }

        try {
            // Parse URL to validate it
            java.net.URI uri = new java.net.URI(redirectUrl);

            // Only allow relative URLs or URLs from your domain
            if (uri.isAbsolute()) {
                String host = uri.getHost();
                if (host == null || !isAllowedHost(host)) {
                    log.warn("Rejected redirect to unauthorized host: {}", host);
                    return null;
                }
            }

            // Additional sanitization
            String sanitized = redirectUrl.replaceAll("[<>\"']", "");

            log.debug("Redirect URL sanitized: {} -> {}", redirectUrl, sanitized);
            return sanitized;

        } catch (Exception e) {
            log.warn("Invalid redirect URL rejected: {}", redirectUrl, e);
            return null;
        }
    }

    private boolean isAllowedHost(String host) {
        // Define allowed hosts for redirect

        Set<String> allowedHosts = Set.of(allowedOrigins.split(","));

        return allowedHosts.contains(host.toLowerCase());
    }

    private void redirectWithSuccess(HttpServletResponse response, AuthResponse authResponse, String customRedirectUrl)
            throws IOException {

        try {
            // Determine target URL
            String targetUrl = determineTargetUrl(customRedirectUrl);
            // Build redirect URL with tokens
            StringBuilder redirectUrl = new StringBuilder(targetUrl);
            String separator = targetUrl.contains("?") ? "&" : "?";

            redirectUrl.append(separator)
                    .append("access_token=").append(urlEncode(authResponse.getAccessToken()))
                    .append("&refresh_token=").append(urlEncode(authResponse.getRefreshToken()))
                    .append("&token_type=").append(urlEncode(authResponse.getTokenType()))
                    .append("&expires_in=").append(authResponse.getExpiresIn());

            // Add user info if available
            if (authResponse.getUser().getEmail() != null) {
                redirectUrl.append("&email=").append(urlEncode(authResponse.getUser().getEmail()));
            }

            if (authResponse.getUser().getName() != null) {
                redirectUrl.append("&name=").append(urlEncode(authResponse.getUser().getName()));
            }

            if (authResponse.getUser().getId() != null) {
                redirectUrl.append("&user_id=").append(urlEncode(authResponse.getUser().getId()));
            }

            if(authResponse.getUser() != null) {
                redirectUrl.append("&user=").append(authResponse.getUser().toString());
            }

            // Add success indicator
            redirectUrl.append("&auth_success=true&provider=linkedin");

            String finalUrl = redirectUrl.toString();
            log.info("Redirecting to success URL: {}", finalUrl.replaceAll("(access_token|refresh_token)=[^&]*", "$1=***"));

            log.info("Redirecting to success URL: {}", finalUrl);
            response.sendRedirect(finalUrl);

        } catch (Exception e) {
            log.error("Failed to redirect with success", e);
            redirectWithError(response, "redirect_failed", "Authentication succeeded but redirect failed");
        }
    }


    private void redirectWithError(HttpServletResponse response, String errorCode, String errorMessage)
            throws IOException {

        try {
            String errorUrl = frontendUrl + loginErrorPath;
            String separator = errorUrl.contains("?") ? "&" : "?";

            String finalUrl = errorUrl + separator +
                    "error=" + urlEncode(errorCode) +
                    "&error_description=" + urlEncode(errorMessage) +
                    "&provider=linkedin";

            log.info("Redirecting to error URL with code: {}", errorCode);
            response.sendRedirect(finalUrl);

        } catch (Exception e) {
            log.error("Failed to redirect with error", e);
            // Fallback to basic error page
            response.sendRedirect(frontendUrl + "/index.html?error=system_error");
        }
    }

    private String determineTargetUrl(String customRedirectUrl) {
        if (customRedirectUrl != null && !customRedirectUrl.trim().isEmpty()) {
            // Use custom redirect URL if provided and valid
            return customRedirectUrl;
        }

        // Default to configured success path
        return frontendUrl + loginSuccessPath;
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to URL encode value", e);
            return value;
        }
    }
    private record StateInfo(String originalState, String redirectUrl) {}


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
