package com.urutare.sso.controllers;

import com.urutare.sso.dto.TokenDto;
import com.urutare.sso.dto.TokenRequest;
import com.urutare.sso.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sso-service")
public class JwtController {

    private final JwtService jwtService;

    @Autowired
    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestBody TokenRequest tokenRequest) {
        try {
            String token = jwtService.generateToken(tokenRequest);
            Map<String, String> tokenResponse = new HashMap<>();
            tokenResponse.put("token", token);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating token: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestBody TokenDto payload) {
        try {
            Map<String, Object> result = jwtService.getTokenPayload(payload);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "false");
            return ResponseEntity.ok(error);
        }
    }


}
