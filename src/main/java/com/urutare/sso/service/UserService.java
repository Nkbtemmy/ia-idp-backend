package com.urutare.sso.service;

import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.entity.VerificationToken;
import com.urutare.sso.enums.AuthProvider;
import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.repository.VerificationTokenRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final VerificationTokenRepository tokenRepository;
  private final PasswordEncoder encoder;
  private final JavaMailSender mailSender;

  public UserService(UserRepository userRepository, VerificationTokenRepository tokenRepository, PasswordEncoder encoder, JavaMailSender mailSender) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.encoder = encoder;
    this.mailSender = mailSender;
  }

  @Transactional
  public void registerLocal(String email, String rawPassword, String appRedirect) {
    userRepository.findByEmailIgnoreCase(email).ifPresent(u -> { throw new RuntimeException("Email already registered"); });
    UserAccount user = new UserAccount();
    user.setEmail(email.toLowerCase());
    user.setPassword(encoder.encode(rawPassword));
    user.setProvider(AuthProvider.LOCAL);
    user.setEmailVerified(false);
    userRepository.save(user);
    sendVerification(user, appRedirect);
  }

  public void sendVerification(UserAccount user, String appRedirect) {
    String token = UUID.randomUUID().toString();
    Instant exp = Instant.now().plus(24, ChronoUnit.HOURS);
    tokenRepository.save(new VerificationToken(token, user, exp));

    try {
      String url = "/api/v1/auth/verify?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) +
          ((appRedirect != null && !appRedirect.isBlank()) ? "&redirect=" + URLEncoder.encode(appRedirect, StandardCharsets.UTF_8) : "");
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setTo(user.getEmail());
      helper.setSubject("Verify your email");
      helper.setText("<p>Click to verify: <a href='" + url + "'>Verify Email</a></p>", true);
      mailSender.send(message);
    } catch (Exception e) {
      throw new RuntimeException("Failed to send verification email", e);
    }
  }

  @Transactional
  public String verify(String token) {
    VerificationToken vt = tokenRepository.findByToken(token)
        .orElseThrow(() -> new RuntimeException("Invalid token"));
    if (vt.isUsed()) throw new RuntimeException("Token already used");
    if (vt.getExpiresAt().isBefore(Instant.now())) throw new RuntimeException("Token expired");

    UserAccount user = vt.getUser();
    user.setEmailVerified(true);
    tokenRepository.delete(vt);
    return user.getEmail();
  }

  public DefaultOAuth2UserService oauth2UserService() {
    return new DefaultOAuth2UserService() {
      @Override
      public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attrs = oAuth2User.getAttributes();
        String providerId = String.valueOf(attrs.getOrDefault("id", attrs.get("sub")));
        String email = (String) attrs.getOrDefault("emailAddress", attrs.get("email"));
        String name = (String) attrs.getOrDefault("localizedFirstName", attrs.getOrDefault("name", "User"));
        provisionLinkedInUser(providerId, email, name);
        return oAuth2User;
      }
    };
  }

  private void provisionLinkedInUser(String providerId, String email, String name) {
    if (email == null || email.isBlank()) {
      return;
    }
    Optional<UserAccount> existing = userRepository.findByEmailIgnoreCase(email);
    if (existing.isPresent()) {
      UserAccount user = existing.get();
      user.setProvider(AuthProvider.LINKEDIN);
      user.setProviderId(providerId);
      user.setEmailVerified(true);
      user.setName(name);
      userRepository.save(user);
    } else {
      UserAccount user = new UserAccount();
      user.setEmail(email.toLowerCase());
      user.setPassword(encoder.encode(UUID.randomUUID().toString()));
      user.setProvider(AuthProvider.LINKEDIN);
      user.setProviderId(providerId);
      user.setEmailVerified(true);
      user.setName(name);
      userRepository.save(user);
    }
  }

  public AuthenticationSuccessHandler oauth2SuccessHandler() {
    return (request, response, authentication) -> {
      response.sendRedirect("/");
    };
  }
}
