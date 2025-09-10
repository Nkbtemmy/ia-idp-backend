package com.urutare.sso.repository;

import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
  Optional<VerificationToken> findByToken(String token);
  
  @Modifying
  @Query("DELETE FROM VerificationToken vt WHERE vt.user = :user")
  void deleteByUser(UserAccount user);
}
