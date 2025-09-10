package com.urutare.sso.config;

import com.urutare.sso.service.JwtService;
import com.urutare.sso.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

  private final UserService userService;
  private final JwtService jwtService;  // Change from JwtAuthenticationFilter to JwtService
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(UserService userService,
                        JwtService jwtService,  // Change constructor to inject JwtService
                        CorsConfigurationSource corsConfigurationSource) {
    this.userService = userService;
    this.jwtService = jwtService;  // Assign JwtService
    this.corsConfigurationSource = corsConfigurationSource;
  }

  // Public endpoints that do not require authentication
  private final List<String> publicPaths = List.of(
          "/api/v1/sso-service/**",
          "/api/v1/sso-service/docs/**",
          "/api/v1/sso-service/swagger-ui/**",
          "/error",
          "/actuator/health",
          "/.well-known/**",
          "/verified.html"
  );

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Create JwtAuthenticationFilter dynamically and pass JwtService
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService());

    http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            // Add JWT filter before Spring Security’s authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return userService::findByEmail;
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService());
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }
}
