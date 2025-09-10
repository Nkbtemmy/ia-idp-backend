package com.urutare.sso.config;

import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.AuthProvider;
import com.urutare.sso.enums.Role;
import com.urutare.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("Database is empty. Seeding initial data...");
            createSeedData();
            log.info("Seed data created successfully!");
        } else {
            log.info("Database already contains data. Skipping seed data creation.");
        }
    }

    private void createSeedData() {
        // Create Admin User
        createUser(
            "admin@sso.com",
            "admin123",
            "System Administrator",
            Set.of(Role.ROLE_ADMIN, Role.ROLE_USER),
            true,
            AuthProvider.LOCAL
        );

        // Create Regular User (verified)
        createUser(
            "user@example.com",
            "user123",
            "John Doe",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL
        );

        // Create Unverified User
        createUser(
            "unverified@example.com",
            "unverified123",
            "Jane Smith",
            Set.of(Role.ROLE_USER),
            false,
            AuthProvider.LOCAL
        );

        // Create LinkedIn User (simulated)
        createUser(
            "linkedin@example.com",
            "linkedin123",
            "LinkedIn User",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LINKEDIN
        );

        // Create Test Users for different scenarios
        createUser(
            "test1@example.com",
            "test123",
            "Test User 1",
            Set.of(Role.ROLE_USER),
            true,
            AuthProvider.LOCAL
        );

        createUser(
            "test2@example.com",
            "test123",
            "Test User 2",
            Set.of(Role.ROLE_USER),
            false,
            AuthProvider.LOCAL
        );
    }

    private void createUser(String email, String password, String name, Set<Role> roles, 
                           boolean emailVerified, AuthProvider provider) {
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

        if (provider == AuthProvider.LINKEDIN) {
            user.setProviderId("linkedin_" + System.currentTimeMillis());
        }

        userRepository.save(user);
        log.info("Created user: {} with roles: {} (verified: {})", email, roles, emailVerified);
    }
}
