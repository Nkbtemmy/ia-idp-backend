package com.urutare.sso.service;

import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.entity.VerificationToken;
import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.repository.VerificationTokenRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.name:SSO Service}")
    private String appName;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.verification.token.expiration:24}")
    private int tokenExpirationHours;

    @Transactional
    public void sendVerificationEmail(UserAccount user) {
        try {
            // Delete any existing verification tokens for this user
            verificationTokenRepository.deleteByUser(user);

            // Create new verification token
            String token = UUID.randomUUID().toString();
            System.out.println("Generated token--------: " + token); // Debugging line
            VerificationToken verificationToken = new VerificationToken(
                token,
                user,
                Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS)
            );
            verificationTokenRepository.save(verificationToken);

            // Send email
            String verificationUrl = frontendUrl + "/verify-email?token=" + token;
            sendEmail(user.getEmail(), "Verify Your Email Address", 
                     createVerificationEmailContent(user.getName() != null ? user.getName() : user.getEmail(), verificationUrl));

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Transactional
    public boolean verifyEmail(String token) {
        Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);
        
        if (verificationTokenOpt.isEmpty()) {
            log.warn("Verification token not found: {}", token);
            return false;
        }

        VerificationToken verificationToken = verificationTokenOpt.get();
        
        if (verificationToken.isUsed()) {
            log.warn("Verification token already used: {}", token);
            return false;
        }

        if (Instant.now().isAfter(verificationToken.getExpiresAt())) {
            log.warn("Verification token expired: {}", token);
            return false;
        }

        // Mark user as verified
        UserAccount user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Optional<UserAccount> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        UserAccount user = userOpt.get();
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        sendVerificationEmail(user);
    }

    private void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        try {
            helper.setFrom(fromEmail, appName);
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        
        mailSender.send(message);
    }

    private String createVerificationEmailContent(String userName, String verificationUrl) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("appName", appName);
        context.setVariable("expirationHours", tokenExpirationHours);
        
        return templateEngine.process("verification-email", context);
    }
}
