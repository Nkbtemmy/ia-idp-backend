package com.urutare.sso.controllers;

import com.urutare.sso.dto.ApiResponse;
import com.urutare.sso.dto.ClientRegistrationRequest;
import com.urutare.sso.entity.ClientApp;
import com.urutare.sso.service.ClientAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sso-service/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientAppService clientAppService;

    @PostMapping("/register")
    public ResponseEntity<?> registerClient(@RequestBody ClientRegistrationRequest request) {
        try {
            ClientApp client = clientAppService.registerClient(
                request.getName(),
                request.getDescription(),
                request.getRedirectUri()
            );
            
            return ResponseEntity.ok(Map.of(
                "clientId", client.getClientId(),
                "clientSecret", client.getClientSecret(),
                "message", "Client registered successfully. Store the client secret securely - it won't be shown again."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Client registration failed: " + e.getMessage()));
        }
    }
}
