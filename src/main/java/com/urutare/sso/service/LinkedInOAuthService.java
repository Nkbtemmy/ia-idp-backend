package com.urutare.sso.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urutare.sso.dto.AuthResponse;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.AuthProvider;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInOAuthService {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.linkedin.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.linkedin.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.linkedin.redirect-uri}")
    private String redirectUri;

    // Updated URLs for LinkedIn API v2
    @Value("${linkedin.oauth.token-url:https://www.linkedin.com/oauth/v2/accessToken}")
    private String tokenUrl;

    // Updated API endpoints - LinkedIn deprecated v2/people endpoint
    @Value("${linkedin.oauth.user-info-url:https://api.linkedin.com/v2/userinfo}")
    private String userInfoUrl;

    // Backup profile URL if userinfo doesn't work
    @Value("${linkedin.oauth.profile-url:https://api.linkedin.com/v2/people/~}")
    private String profileUrl;

    public String getAuthorizationUrl(String state) {
        log.info("Generating LinkedIn OAuth URL with state: {}", state);

        try {
            // Updated scopes - LinkedIn deprecated r_liteprofile
            String scope = "openid profile email";
            log.info("Redirect URI: {}", redirectUri);
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);
            String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);

            String authUrl = String.format(
                    "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                    clientId, encodedRedirectUri, encodedState, encodedScope
            );

            log.info("LinkedIn OAuth URL generated successfully");
            return authUrl;

        } catch (Exception e) {
            log.error("Error generating LinkedIn OAuth URL", e);
            throw new RuntimeException("Failed to generate LinkedIn authorization URL: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse handleCallback(String code, String state) {
        try {
            log.info("Processing LinkedIn callback with code: {}, state: {}",
                    code != null ? "***" : null, state);

            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Authorization code is required");
            }

            // Exchange code for access token
            String accessToken = exchangeCodeForToken(code);

            // Get user info from LinkedIn
            LinkedInUserInfo userInfo = getUserInfo(accessToken);

            // Find or create user
            UserAccount user = findOrCreateUser(userInfo);

            // Generate authentication response
            return authenticationService.generateAuthResponse(user);

        } catch (Exception e) {
            log.error("LinkedIn OAuth callback failed", e);
            throw new RuntimeException("LinkedIn authentication failed: " + e.getMessage());
        }
    }

    private String exchangeCodeForToken(String code) {
        try {
            log.info("Exchanging authorization code for access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Token exchange failed. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to exchange code for token: " + response.getStatusCode());
            }

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (!jsonNode.has("access_token")) {
                log.error("No access token in response: {}", response.getBody());
                throw new RuntimeException("Access token not found in LinkedIn response");
            }

            String accessToken = jsonNode.get("access_token").asText();
            log.info("Access token obtained successfully");
            return accessToken;

        } catch (HttpClientErrorException e) {
            log.error("HTTP error during token exchange: {}", e.getResponseBodyAsString());
            throw new RuntimeException("LinkedIn token exchange failed: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("Network error during token exchange", e);
            throw new RuntimeException("Network error during LinkedIn authentication: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during token exchange", e);
            throw new RuntimeException("Token exchange failed: " + e.getMessage());
        }
    }

    private LinkedInUserInfo getUserInfo(String accessToken) {
        try {
            log.info("Fetching user information from LinkedIn");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Try the new userinfo endpoint first (OpenID Connect standard)
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        userInfoUrl, HttpMethod.GET, entity, String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    return parseUserInfoResponse(response.getBody());
                }
            } catch (HttpClientErrorException e) {
                log.warn("Userinfo endpoint failed, trying profile endpoint. Error: {}", e.getMessage());
            }

            // Fallback to profile endpoint
            ResponseEntity<String> profileResponse = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, entity, String.class
            );

            if (!profileResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to get LinkedIn profile. Status: {}, Body: {}",
                        profileResponse.getStatusCode(), profileResponse.getBody());
                throw new RuntimeException("Failed to get user profile from LinkedIn");
            }

            return parseProfileResponse(profileResponse.getBody(), accessToken, entity);

        } catch (Exception e) {
            log.error("Error fetching LinkedIn user info", e);
            throw new RuntimeException("Failed to fetch user information from LinkedIn: " + e.getMessage());
        }
    }

    private LinkedInUserInfo parseUserInfoResponse(String responseBody) throws Exception {
        log.info("Parsing userinfo response: {}", responseBody);
        JsonNode userNode = objectMapper.readTree(responseBody);

        String email = userNode.has("email") ? userNode.get("email").asText() : null;
        String firstName = userNode.has("given_name") ? userNode.get("given_name").asText() : "";
        String lastName = userNode.has("family_name") ? userNode.get("family_name").asText() : "";
        String fullName = userNode.has("name") ? userNode.get("name").asText() : (firstName + " " + lastName).trim();
        String linkedInId = userNode.has("sub") ? userNode.get("sub").asText() : null;

        if (email == null || linkedInId == null) {
            throw new RuntimeException("Required user information missing from LinkedIn response");
        }

        if (fullName.isEmpty()) {
            fullName = email.split("@")[0]; // Fallback to email prefix
        }

        log.info("Successfully parsed user info: ID={}, Email={}, Name={}", linkedInId, email, fullName);
        return new LinkedInUserInfo(linkedInId, email, fullName, firstName, lastName);
    }

    private LinkedInUserInfo parseProfileResponse(String profileBody, String accessToken, HttpEntity<String> entity) throws Exception {
        log.info("Parsing profile response: {}", profileBody);
        JsonNode profileNode = objectMapper.readTree(profileBody);

        // Extract basic info
        String linkedInId = profileNode.has("id") ? profileNode.get("id").asText() : null;
        if (linkedInId == null) {
            throw new RuntimeException("LinkedIn user ID not found");
        }

        // Extract names with better handling
        String firstName = extractLocalizedName(profileNode, "firstName");
        String lastName = extractLocalizedName(profileNode, "lastName");
        String fullName = (firstName + " " + lastName).trim();

        // Get email separately (LinkedIn v2 API requires separate call for email)
        String email = getEmailFromLinkedIn(accessToken, entity);

        if (fullName.isEmpty()) {
            fullName = email.split("@")[0]; // Fallback to email prefix
        }

        log.info("Successfully parsed profile info: ID={}, Email={}, Name={}", linkedInId, email, fullName);
        return new LinkedInUserInfo(linkedInId, email, fullName, firstName, lastName);
    }

    private String extractLocalizedName(JsonNode profileNode, String fieldName) {
        if (!profileNode.has(fieldName)) return "";

        JsonNode nameNode = profileNode.get(fieldName);
        if (!nameNode.has("localized")) return "";

        JsonNode localizedNode = nameNode.get("localized");

        // Try common locales
        String[] locales = {"en_US", "en-US", "en"};
        for (String locale : locales) {
            if (localizedNode.has(locale)) {
                return localizedNode.get(locale).asText();
            }
        }

        // Return first available locale
        if (localizedNode.size() > 0) {
            return localizedNode.elements().next().asText();
        }

        return "";
    }

    private String getEmailFromLinkedIn(String accessToken, HttpEntity<String> entity) {
        try {
            String emailEndpoint = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";

            ResponseEntity<String> emailResponse = restTemplate.exchange(
                    emailEndpoint, HttpMethod.GET, entity, String.class
            );

            if (!emailResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to get email from LinkedIn");
            }

            JsonNode emailNode = objectMapper.readTree(emailResponse.getBody());

            if (emailNode.has("elements") && emailNode.get("elements").isArray() &&
                    emailNode.get("elements").size() > 0) {
                JsonNode firstElement = emailNode.get("elements").get(0);
                if (firstElement.has("handle~") && firstElement.get("handle~").has("emailAddress")) {
                    return firstElement.get("handle~").get("emailAddress").asText();
                }
            }

            throw new RuntimeException("Could not extract email from LinkedIn response");

        } catch (Exception e) {
            log.error("Failed to get email from LinkedIn", e);
            throw new RuntimeException("Failed to get user email: " + e.getMessage());
        }
    }


    private UserAccount findOrCreateUser(LinkedInUserInfo userInfo) {
        log.info("Finding or creating user for LinkedIn ID: {}", userInfo.linkedInId());

        // 1. Try to find user by LinkedIn ID
        Optional<UserAccount> existingByLinkedInId = userRepository.findByProviderIdAndProvider(
                userInfo.linkedInId(), AuthProvider.LINKEDIN
        );

        if (existingByLinkedInId.isPresent()) {
            UserAccount user = existingByLinkedInId.get();
            log.info("Found existing LinkedIn user: {}", user.getEmail());

            // Update info if needed
            boolean updated = false;
            if (!userInfo.fullName().equals(user.getName())) {
                user.setName(userInfo.fullName());
                updated = true;
            }
            if (!userInfo.email().equalsIgnoreCase(user.getEmail())) {
                user.setEmail(userInfo.email().toLowerCase());
                updated = true;
            }

            if (updated) {
                user = userRepository.save(user);
                log.info("Updated existing LinkedIn user information");
            }
            return user;
        }

        // 2. Try to find user by email
        Optional<UserAccount> existingByEmail = userRepository.findByEmailIgnoreCase(userInfo.email());
        if (existingByEmail.isPresent()) {
            UserAccount user = existingByEmail.get();
            log.info("Found user by email: {}, linking LinkedIn account", user.getEmail());

            // Link LinkedIn to this account
            user.setProviderId(userInfo.linkedInId());
            user.setProvider(AuthProvider.LINKEDIN);
            user.setName(userInfo.fullName());
            user.setEmailVerified(true);

            return userRepository.save(user);
        }

        // 3. Create new user
        log.info("Creating new LinkedIn user: {}", userInfo.email());
        UserAccount newUser = UserAccount.builder()
                .email(userInfo.email().toLowerCase())
                .name(userInfo.fullName())
                .provider(AuthProvider.LINKEDIN)
                .providerId(userInfo.linkedInId())
                .emailVerified(true) // LinkedIn emails are always verified
                .roles(Set.of(Role.ROLE_USER))
                .password("") // No password for OAuth users
                .build();

        UserAccount savedUser = userRepository.save(newUser);
        log.info("Created new LinkedIn user with ID: {}", savedUser.getId());
        return savedUser;
    }


    private record LinkedInUserInfo(
            String linkedInId,
            String email,
            String fullName,
            String firstName,
            String lastName
    ) {}
}