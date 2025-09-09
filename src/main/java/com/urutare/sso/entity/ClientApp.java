package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_apps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String clientId;
    
    @Column(nullable = false)
    private String clientSecret;
    
    @Column(nullable = false)
    private String name;
    
    @Column
    private String description;
    
    @Column(nullable = false)
    private String redirectUri;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
