package com.urutare.sso.service;

import com.urutare.sso.entity.ClientApp;
import com.urutare.sso.repository.ClientAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAppService {

    private final ClientAppRepository clientAppRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientApp registerClient(String name, String description, String redirectUri) {
        String clientId = "client_" + UUID.randomUUID().toString().replace("-", "");
        String clientSecret = UUID.randomUUID().toString();

        ClientApp clientApp = ClientApp.builder()
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .name(name)
                .description(description)
                .redirectUri(redirectUri)
                .build();

        ClientApp saved = clientAppRepository.save(clientApp);
        
        // Return with plain text secret for initial setup
        saved.setClientSecret(clientSecret);
        return saved;
    }

    public boolean validateClient(String clientId, String clientSecret) {
        return clientAppRepository.findByClientIdAndActive(clientId, true)
                .map(client -> passwordEncoder.matches(clientSecret, client.getClientSecret()))
                .orElse(false);
    }

    public ClientApp getClientByClientId(String clientId) {
        return clientAppRepository.findByClientIdAndActive(clientId, true)
                .orElseThrow(() -> new RuntimeException("Client not found or inactive"));
    }
}
