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
import org.springframework.web.client.RestTemplate;

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

    @Value("${linkedin.oauth.token-url:https://www.linkedin.com/oauth/v2/accessToken}")
    private String tokenUrl;

    @Value("${linkedin.oauth.user-info-url:https://api.linkedin.com/v2/people/~:(id,firstName,lastName)}")
    private String userInfoUrl;

    @Value("${linkedin.oauth.email-url:https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))}")
    private String emailUrl;


    public String getAuthorizationUrl(String state) {
        log.info("Generating LinkedIn OAuth URL with Client ID: {}", clientId);
        String scope = "r_liteprofile r_emailaddress";
        String authUrl = String.format(
            "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
            clientId, redirectUri, state, scope
        );
        log.info("LinkedIn OAuth URL: {}", authUrl);
        return authUrl;
    }

    @Transactional
    public AuthResponse handleCallback(String code, String state) {
        try {
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

    private String exchangeCodeForToken(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to exchange code for token");
        }

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("access_token").asText();
    }

    private LinkedInUserInfo getUserInfo(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Get basic profile info
            log.info("Fetching LinkedIn profile from: {}", userInfoUrl);
            ResponseEntity<String> profileResponse = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, entity, String.class
            );

            if (profileResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to get LinkedIn profile. Status: {}, Body: {}", 
                    profileResponse.getStatusCode(), profileResponse.getBody());
                throw new RuntimeException("Failed to get user profile from LinkedIn");
            }

            log.info("LinkedIn profile response: {}", profileResponse.getBody());
            JsonNode profileNode = objectMapper.readTree(profileResponse.getBody());

            // Get email address
            log.info("Fetching LinkedIn email from: {}", emailUrl);
            ResponseEntity<String> emailResponse = restTemplate.exchange(
                emailUrl, HttpMethod.GET, entity, String.class
            );

            if (emailResponse.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to get LinkedIn email. Status: {}, Body: {}", 
                    emailResponse.getStatusCode(), emailResponse.getBody());
                throw new RuntimeException("Failed to get user email from LinkedIn");
            }

            log.info("LinkedIn email response: {}", emailResponse.getBody());
            JsonNode emailNode = objectMapper.readTree(emailResponse.getBody());
            
            // Extract email with better error handling
            String email = null;
            if (emailNode.has("elements") && emailNode.get("elements").isArray() && 
                emailNode.get("elements").size() > 0) {
                JsonNode firstElement = emailNode.get("elements").get(0);
                if (firstElement.has("handle~") && firstElement.get("handle~").has("emailAddress")) {
                    email = firstElement.get("handle~").get("emailAddress").asText();
                }
            }
            
            if (email == null || email.isEmpty()) {
                throw new RuntimeException("Could not extract email from LinkedIn response");
            }

            // Extract user information with better error handling
            String linkedInId = profileNode.get("id").asText();
            
            String firstName = "";
            String lastName = "";
            
            if (profileNode.has("firstName") && profileNode.get("firstName").has("localized")) {
                JsonNode firstNameNode = profileNode.get("firstName").get("localized");
                // Try different locale keys
                if (firstNameNode.has("en_US")) {
                    firstName = firstNameNode.get("en_US").asText();
                } else if (firstNameNode.size() > 0) {
                    // Get first available locale
                    firstName = firstNameNode.elements().next().asText();
                }
            }
            
            if (profileNode.has("lastName") && profileNode.get("lastName").has("localized")) {
                JsonNode lastNameNode = profileNode.get("lastName").get("localized");
                // Try different locale keys
                if (lastNameNode.has("en_US")) {
                    lastName = lastNameNode.get("en_US").asText();
                } else if (lastNameNode.size() > 0) {
                    // Get first available locale
                    lastName = lastNameNode.elements().next().asText();
                }
            }
            
            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isEmpty()) {
                fullName = email.split("@")[0]; // Fallback to email prefix
            }

            log.info("Extracted LinkedIn user info: ID={}, Email={}, Name={}", linkedInId, email, fullName);
            return new LinkedInUserInfo(linkedInId, email, fullName, firstName, lastName);
            
        } catch (Exception e) {
            log.error("Error fetching LinkedIn user info", e);
            throw new RuntimeException("Failed to fetch user information from LinkedIn: " + e.getMessage());
        }
    }

    private UserAccount findOrCreateUser(LinkedInUserInfo userInfo) {
        // First, try to find user by LinkedIn ID
        Optional<UserAccount> existingUser = userRepository.findByProviderIdAndProvider(
            userInfo.linkedInId(), AuthProvider.LINKEDIN
        );

        if (existingUser.isPresent()) {
            UserAccount user = existingUser.get();
            // Update user info if needed
            if (!userInfo.fullName().equals(user.getName())) {
                user.setName(userInfo.fullName());
                user = userRepository.save(user);
            }
            return user;
        }

        // Check if user exists with same email but different provider
        Optional<UserAccount> emailUser = userRepository.findByEmailIgnoreCase(userInfo.email());
        if (emailUser.isPresent()) {
            UserAccount user = emailUser.get();
            // Link LinkedIn account to existing user
            user.setProviderId(userInfo.linkedInId());
            user.setProvider(AuthProvider.LINKEDIN);
            user.setName(userInfo.fullName());
            user.setEmailVerified(true); // LinkedIn emails are verified
            return userRepository.save(user);
        }

        // Create new user
        UserAccount newUser = UserAccount.builder()
                .email(userInfo.email().toLowerCase())
                .name(userInfo.fullName())
                .provider(AuthProvider.LINKEDIN)
                .providerId(userInfo.linkedInId())
                .emailVerified(true) // LinkedIn emails are verified
                .roles(Set.of(Role.ROLE_USER))
                .password("") // No password for OAuth users
                .build();

        return userRepository.save(newUser);
    }

    private record LinkedInUserInfo(
        String linkedInId,
        String email,
        String fullName,
        String firstName,
        String lastName
    ) {}
}
