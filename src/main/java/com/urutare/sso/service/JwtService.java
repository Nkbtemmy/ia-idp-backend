package com.urutare.sso.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urutare.sso.dto.TokenDto;
import com.urutare.sso.dto.TokenRequest;
import com.urutare.sso.utils.KeyLoader;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

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

    public String generateToken(TokenRequest subject) throws Exception {
        RSAPrivateKey privateKey = getPrivateKey();
        Algorithm rsaAlgorithm = Algorithm.RSA256(null, privateKey);

        ObjectMapper objectMapper = new ObjectMapper();
        String subjectValue = subject.getSubject(); // Still used as the JWT subject

        // Serialize all TokenRequest fields as claims (except subject)
        Map claims = objectMapper.convertValue(subject, Map.class);
        claims.remove("subject"); // Remove subject if you want it only in .withSubject

        return JWT.create()
                .withSubject(subjectValue)
                .withIssuer(issuer)
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // 1 day expiration
                .withPayload(claims)
                .sign(rsaAlgorithm);
    }

    public void verifyJwtToken(String token) {
        try {
            RSAPublicKey publicKey = getPublicKey();
            System.out.println("Public Key: " + publicKey.toString());
            Algorithm rsaAlgorithm = Algorithm.RSA256(publicKey, null);

            JWTVerifier jwtVerifier = JWT.require(rsaAlgorithm).withIssuer(issuer).build();
            DecodedJWT decodedJWT = jwtVerifier.verify(token);

            // Print decoded JWT details for debugging
            System.out.println("Token Verified Successfully!");
            System.out.println("Issuer: " + decodedJWT.getIssuer());
            System.out.println("Subject: " + decodedJWT.getSubject());
            System.out.println("Expiration: " + decodedJWT.getExpiresAt());

        } catch (JWTVerificationException ex) {
            System.out.println("JWTVerificationException: " + ex.getMessage());
            throw new JWTVerificationException("Invalid Token");
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            throw new JWTVerificationException(ex.getMessage());
        }
    }


@NotNull
public Map<String, Object> getTokenPayload(TokenDto token) {
    String tokens = token.getToken();
    verifyJwtToken(tokens);
    String[] chunks = tokens.split("\\.");
    Base64.Decoder decoder = Base64.getUrlDecoder();
    String payloadJson = new String(decoder.decode(chunks[1]));
    ObjectMapper objectMapper = new ObjectMapper();
    try {
        return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
        throw new RuntimeException("Failed to parse token payload", e);
    }
}
}
