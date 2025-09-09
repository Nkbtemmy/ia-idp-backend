package com.urutare.sso.repository;

import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);
  Optional<UserAccount> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
