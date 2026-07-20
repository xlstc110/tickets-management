package com.tickets.authorizationserver.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.tickets.authorizationserver.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

@Slf4j
@Component
public class KeyUtils {
    private static final String RSA = "RSA";

    @Value("${spring.profiles.active}")
    // If is dev profile, will generate local key and use it to generator token
    private String activeProfile;

    @Value("${keys.private.key}")
    private String privateKey;

    @Value("${keys.public.key}")
    private String publicKey;

    public RSAKey getRSAKeyPair() {
        return generateRSAKeyPair(privateKey, publicKey);
    }

    private RSAKey generateRSAKeyPair(String privateKey, String publicKey) {
        KeyPair keyPair;
        var keysDirectory = Paths.get("src","main","resources","keys");
        verifyKeysDirectory(keysDirectory);
        // If both private and public keys exist, extract them and return the RSAKey
        if(Files.exists(keysDirectory.resolve(privateKey)) && Files.exists(keysDirectory.resolve(publicKey))) {
            log.info("RSA keys exist. Loading keys from files.");
            try{
                File privateKeyPath = keysDirectory.resolve(privateKey).toFile();
                File publicKeyPath = keysDirectory.resolve(publicKey).toFile();
                var keyFactory = KeyFactory.getInstance(RSA);
                byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath.toPath());
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

                byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath.toPath());
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

                var keyId = UUID.randomUUID().toString();
                log.info("Key id is {}", keyId);
                return new RSAKey.Builder(rsaPublicKey)
                        .privateKey(rsaPrivateKey)
                        .keyID(keyId)
                        .build();
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new ApiException("Failed to generate RSA key pair");
            }
        } else {
            // If the files are not exist
            if(activeProfile.equalsIgnoreCase("prod")) {
                throw new ApiException("Required keys not found for production environment.");
            }
        }

        log.info("RSA keys not found. Generating keys.");
        try {
            var keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("RSA algorithm not available");
            throw new ApiException("RSA algorithm not available");
        }
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();

        try(var fos = new FileOutputStream(keysDirectory.resolve(privateKey).toFile())){
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
            fos.write(keySpec.getEncoded());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("Failed to save private key");
        }

        try(var fos = new FileOutputStream(keysDirectory.resolve(publicKey).toFile())){
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
            fos.write(keySpec.getEncoded());
            return new RSAKey.Builder(rsaPublicKey)
                    .privateKey(rsaPrivateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApiException("Failed to save public key");
        }

    }

    private void verifyKeysDirectory(Path keysDirectory) {
        if(!Files.exists(keysDirectory)) {
            try{
                Files.createDirectories(keysDirectory);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new ApiException("Failed to create keys directory");
            }
        }
        log.info("Created key directory: {}", keysDirectory);
    }

}
