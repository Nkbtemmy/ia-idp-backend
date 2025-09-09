package com.urutare.sso.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.urutare.sso.entity.RefreshToken;
import com.urutare.sso.entity.UserAccount;
import com.urutare.sso.repository.RefreshTokenRepository;
import com.urutare.sso.utils.KeyLoader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${private.key.path}")
    private String privateKeyPath;

    @Value("${public.key.path}")
    private String publicKeyPath;

    @Value("${private.key.password}")
    private String privateKeyPassword;

    @Value("${jwt.issuer}")
    private String issuer;

    private RSAPublicKey getPublicKey() throws Exception {
        Resource resource = new ClassPathResource(publicKeyPath);
        return (RSAPublicKey) KeyLoader.loadPublicKey(resource.getInputStream());
    }

    private RSAPrivateKey getPrivateKey() throws Exception {
        Resource resource = new ClassPathResource(privateKeyPath);
        return (RSAPrivateKey) KeyLoader.loadPrivateKey(resource.getInputStream(), privateKeyPassword);
    }

    public String generateAccessToken(UserAccount user, String clientId) throws Exception {
        RSAPrivateKey privateKey = getPrivateKey();
        Algorithm algorithm = Algorithm.RSA256(null, privateKey);

        return JWT.create()
                .withSubject(user.getEmail())
                .withIssuer(issuer)
                .withAudience(clientId)
                .withClaim("userId", user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("roles", user.getRoles().stream().map(Enum::name).toList())
                .withClaim("emailVerified", user.isEmailVerified())
                .withClaim("provider", user.getProvider().name())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutes
                .sign(algorithm);
    }

    @Transactional
    public RefreshToken generateRefreshToken(UserAccount user) {
        // Revoke existing refresh tokens for this user
        refreshTokenRepository.revokeAllByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS)) // 7 days
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Map<String, Object> generateTokenPair(UserAccount user, String clientId) throws Exception {
        String accessToken = generateAccessToken(user, clientId);
        RefreshToken refreshToken = generateRefreshToken(user);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken.getToken());
        tokens.put("token_type", "Bearer");
        tokens.put("expires_in", 900); // 15 minutes in seconds
        return tokens;
    }

    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshTokenValue, String clientId) throws Exception {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        UserAccount user = refreshToken.getUser();
        return generateTokenPair(user, clientId);
    }

    public DecodedJWT verifyAccessToken(String token) throws Exception {
        RSAPublicKey publicKey = getPublicKey();
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);

        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();

        return verifier.verify(token);
    }

    public Map<String, Object> getPublicKeyJWKS() throws Exception {
        RSAPublicKey publicKey = getPublicKey();
        
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", "sso-key-1");
        jwk.put("n", java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray()));
        jwk.put("e", java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getPublicExponent().toByteArray()));

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", java.util.List.of(jwk));
        return jwks;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}
