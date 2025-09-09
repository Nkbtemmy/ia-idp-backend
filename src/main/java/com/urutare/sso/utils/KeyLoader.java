package com.urutare.sso.utils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.operator.InputDecryptorProvider;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class KeyLoader {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static PublicKey loadPublicKey(InputStream inputStream) throws Exception {
        String key = readKey(inputStream);
        key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public static PrivateKey loadPrivateKey(InputStream inputStream, String password) throws Exception {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Object object = pemParser.readObject();

            if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                JceOpenSSLPKCS8DecryptorProviderBuilder decryptorProviderBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC");
                InputDecryptorProvider decryptorProvider = decryptorProviderBuilder.build(password.toCharArray());
                PrivateKeyInfo keyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);
                return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(keyInfo);
            } else if (object instanceof PEMEncryptedKeyPair) {
                PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) object;
                JcePEMDecryptorProviderBuilder decryptorProviderBuilder = new JcePEMDecryptorProviderBuilder().setProvider("BC");
                PEMDecryptorProvider decryptorProvider = decryptorProviderBuilder.build(password.toCharArray());
                PEMKeyPair decryptedKeyPair = encryptedKeyPair.decryptKeyPair(decryptorProvider);
                return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(decryptedKeyPair.getPrivateKeyInfo());
            } else if (object instanceof PEMKeyPair) {
                PEMKeyPair keyPair = (PEMKeyPair) object;
                return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(keyPair.getPrivateKeyInfo());
            } else {
                throw new IllegalArgumentException("Invalid private key format");
            }
        }
    }

    private static String readKey(InputStream inputStream) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
