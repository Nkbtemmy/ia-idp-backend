package com.urutare.sso.config;

import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.service.UserService;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserService userService;
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(UserService userService, CorsConfigurationSource corsConfigurationSource) {
    this.userService = userService;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  List<String> publicPaths = List.of(
        "/api/v1/sso-service/auth/**",
        "/api/v1/sso-service/docs/**",
        "/api/v1/sso-service/swagger-ui/**",
        "/error",
        "/actuator/health",
        "/.well-known/**",
        "/verified.html"
  );

  @Bean
  public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
    http
      .cors(cors -> cors.configurationSource(corsConfigurationSource))
      .csrf(csrf -> csrf.disable())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
        .anyRequest().authenticated()
      )
      .oauth2Login(oauth -> oauth
        .loginPage("/login")
        .userInfoEndpoint(ui -> ui.userService(userService.oauth2UserService()))
        .successHandler(userService.oauth2SuccessHandler())
      );

    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return email -> userRepository.findByEmailIgnoreCase(email)
        .map(user -> {
          if (!user.isEnabled()) {
            throw new RuntimeException("Email not verified");
          }
          return user;
        })
        .orElseThrow(() -> new RuntimeException("User not found"));
  }


  @Bean
  public DaoAuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(uds);
    provider.setPasswordEncoder(encoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }
}
