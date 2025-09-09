package com.urutare.sso.repository;

import com.urutare.sso.entity.ClientApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientAppRepository extends JpaRepository<ClientApp, UUID> {
    
    Optional<ClientApp> findByClientId(String clientId);
    
    Optional<ClientApp> findByClientIdAndActive(String clientId, boolean active);
}
