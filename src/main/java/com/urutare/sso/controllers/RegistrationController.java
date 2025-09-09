package com.urutare.sso.controllers;


import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.dto.RegisterRequest;
import com.urutare.sso.repository.UserRepository;
import com.urutare.sso.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sso-service/auth")
@AllArgsConstructor
public class RegistrationController {
  private final UserService userService;
  private final UserRepository userRepository;

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
    try {
      userService.registerLocal(req.getEmail(), req.getPassword(), req.getRedirect());
      return ResponseEntity.ok(ApiResponse.ok("Registration successful. Please verify your email.", null));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }
  }

  @GetMapping("/verify")
  public void verify(@RequestParam String token, @RequestParam(required = false) String redirect, HttpServletResponse response) throws Exception {
    String email = userService.verify(token);
    if (redirect != null && !redirect.isBlank()) {
      String sep = redirect.contains("?") ? "&" : "?";
      response.sendRedirect(redirect + sep + "verified=1&email=" + email);
    } else {
      response.sendRedirect("/verified.html");
    }
  }
}
