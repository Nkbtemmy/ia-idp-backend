package com.urutare.sso.service;

import com.urutare.sso.entity.RefreshToken;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.entity.VerificationToken;
import com.urutare.sso.enums.AuthProvider;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.RefreshTokenRepository;
import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedDataService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createDevelopmentData() {
        log.info("Creating development seed data...");

        // Create comprehensive test users
        createTestUsers();
        
        // Create verification tokens for unverified users
        createVerificationTokens();
        
        log.info("Development seed data created successfully!");
    }

    private void createTestUsers() {
        // Super Admin
        createUserIfNotExists(
            "superadmin@sso.com",
            "SuperAdmin123!",
            "Super Administrator",
            Set.of(Role.ROLE_ADMIN, Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Super admin with all privileges"
        );

        // Regular Admin
        createUserIfNotExists(
            "admin@sso.com", 
            "Admin123!",
            "System Admin",
            Set.of(Role.ROLE_ADMIN, Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "System administrator"
        );

        // Verified Regular Users
        createUserIfNotExists(
            "john.doe@example.com",
            "User123!",
            "John Doe",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Regular verified user"
        );

        createUserIfNotExists(
            "jane.smith@example.com",
            "User123!",
            "Jane Smith", 
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Regular verified user"
        );

        // Unverified Users (for testing email verification)
        createUserIfNotExists(
            "unverified1@example.com",
            "Test123!",
            "Unverified User 1",
            Set.of(Role.ROLE_USER),
            false,
            AuthProvider.LOCAL,
            "User pending email verification"
        );

        createUserIfNotExists(
            "unverified2@example.com",
            "Test123!",
            "Unverified User 2",
            Set.of(Role.ROLE_USER),
            false,
            AuthProvider.LOCAL,
            "User pending email verification"
        );

        // LinkedIn OAuth Users
        createUserIfNotExists(
            "linkedin.user@example.com",
            "LinkedIn123!",
            "LinkedIn User",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LINKEDIN,
            "User registered via LinkedIn OAuth"
        );

        // Test Users for different scenarios
        createUserIfNotExists(
            "test.user1@test.com",
            "Test123!",
            "Test User 1",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Test user for API testing"
        );

        createUserIfNotExists(
            "test.user2@test.com",
            "Test123!",
            "Test User 2",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Test user for API testing"
        );

        // Demo Users
        createUserIfNotExists(
            "demo@sso.com",
            "Demo123!",
            "Demo User",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL,
            "Demo user for presentations"
        );
    }

    private void createUserIfNotExists(String email, String password, String name, 
                                     Set<Role> roles, boolean emailVerified, 
                                     AuthProvider provider, String description) {
        if (userRepository.findByEmailIgnoreCase(email).isEmpty()) {
            UserAccount user = UserAccount.builder()
                    .email(email.toLowerCase())
                    .password(passwordEncoder.encode(password))
                    .name(name)
                    .roles(roles)
                    .emailVerified(emailVerified)
                    .provider(provider)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            // Set provider ID for OAuth users
            if (provider == AuthProvider.LINKEDIN) {
                user.setProviderId("linkedin_" + UUID.randomUUID().toString().substring(0, 8));
            }

            userRepository.save(user);
            log.info("Created user: {} - {} ({})", email, description, 
                    emailVerified ? "verified" : "unverified");
        } else {
            log.debug("User already exists: {}", email);
        }
    }

    private void createVerificationTokens() {
        // Create verification tokens for unverified users
        userRepository.findAll().stream()
                .filter(user -> !user.isEmailVerified() && user.getProvider() == AuthProvider.LOCAL)
                .forEach(user -> {
                    String token = UUID.randomUUID().toString();
                    VerificationToken verificationToken = new VerificationToken(
                            token,
                            user,
                            Instant.now().plus(24, ChronoUnit.HOURS)
                    );
                    verificationTokenRepository.save(verificationToken);
                    log.info("Created verification token for user: {} - Token: {}", 
                            user.getEmail(), token);
                });
    }

    @Transactional
    public void clearAllData() {
        log.warn("Clearing all data from database...");
        refreshTokenRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
        log.warn("All data cleared from database!");
    }

    public void printSeedDataSummary() {
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.isEmailVerified() ? 1 : 0)
                .sum();
        long unverifiedUsers = totalUsers - verifiedUsers;
        long adminUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.getRoles().contains(Role.ROLE_ADMIN) ? 1 : 0)
                .sum();
        long linkedinUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.getProvider() == AuthProvider.LINKEDIN ? 1 : 0)
                .sum();

        log.info("=== SEED DATA SUMMARY ===");
        log.info("Total Users: {}", totalUsers);
        log.info("Verified Users: {}", verifiedUsers);
        log.info("Unverified Users: {}", unverifiedUsers);
        log.info("Admin Users: {}", adminUsers);
        log.info("LinkedIn Users: {}", linkedinUsers);
        log.info("Verification Tokens: {}", verificationTokenRepository.count());
        log.info("========================");
    }
}
