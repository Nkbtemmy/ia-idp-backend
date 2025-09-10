package com.urutare.sso.repository;

import com.urutare.sso.entity.RefreshToken;
import com.urutare.sso.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByToken(String token);
    
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteByUser(UserAccount user);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(UserAccount user);
}
