package com.urutare.sso.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_token")
@Getter
@Setter
public class VerificationToken {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(500)")
  private String token;

  @ManyToOne(optional = false)
  private UserAccount user;

  @Column(nullable = false)
  private Instant expiresAt;

  private boolean used;

  public VerificationToken() {}
  public VerificationToken(String token, UserAccount user, Instant expiresAt) {
    this.token = token; this.user = user; this.expiresAt = expiresAt; this.used = false;
  }

}
