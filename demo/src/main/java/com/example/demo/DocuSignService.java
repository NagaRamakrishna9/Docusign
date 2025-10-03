package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class DocuSignService {

    @Value("${docusign.integration-key}")
    private String clientId;

    @Value("${docusign.user-id}")
    private String userId;

    @Value("${docusign.account-id}")
    private String accountId;

    @Value("${docusign.private-key-path}")
    private Resource privateKeyPath;

    @Value("${docusign.base-path}")
    private String baseUrl;

    private WebClient webClient;
    private String accessToken;

    @PostConstruct
    public void init() {
        try {
            // Read private key PEM
            String keyPem;
            try (InputStream is = privateKeyPath.getInputStream()) {
                keyPem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            keyPem = keyPem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                           .replace("-----END RSA PRIVATE KEY-----", "")
                           .replaceAll("\\s+", "");

            byte[] pkcs1Bytes = Base64.getDecoder().decode(keyPem);
            byte[] pkcs8Bytes = convertPkcs1ToPkcs8(pkcs1Bytes);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);

            // Sign JWT
            RSASSASigner signer = new RSASSASigner(privateKey);
            Instant now = Instant.now();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),
                    new com.nimbusds.jwt.JWTClaimsSet.Builder()
                            .issuer(clientId)
                            .subject(userId)
                            .audience("account-d.docusign.com")
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(now.plusSeconds(3600)))
                            .claim("scope", "signature")
                            .build()
            );
            jwt.sign(signer);

            String jwtToken = jwt.serialize();

            // Exchange JWT for access token
            WebClient tokenClient = WebClient.builder()
                    .baseUrl("https://account-d.docusign.com")
                    .build();

            Map<String, Object> tokenResponse = tokenClient.post()
                    .uri("/oauth/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwtToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain DocuSign access token");
            }

            accessToken = (String) tokenResponse.get("access_token");

            // Create WebClient for API calls
            webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DocuSignService", e);
        }
    }

    private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        // Wrap PKCS#1 inside PKCS#8 (ASN.1 DER encoding)
        final byte[] pkcs8Header = new byte[]{
                0x30, (byte) 0x82,
                (byte) ((pkcs1Bytes.length + 22) >> 8), (byte) (pkcs1Bytes.length + 22),
                0x02, 0x01, 0x00,
                0x30, 0x0D,
                0x06, 0x09,
                0x2A, (byte) 0x86, 0x48, (byte) 0x86,
                (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, (byte) 0x82,
                (byte) (pkcs1Bytes.length >> 8), (byte) (pkcs1Bytes.length)
        };
        byte[] pkcs8Bytes = new byte[pkcs8Header.length + pkcs1Bytes.length];
        System.arraycopy(pkcs8Header, 0, pkcs8Bytes, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pkcs8Header.length, pkcs1Bytes.length);
        return pkcs8Bytes;
    }

    public WebClient getWebClient() {
        if (webClient == null) {
            throw new IllegalStateException("DocuSignService not initialized properly");
        }
        return webClient;
    }

    public String getAccountId() {
        return accountId;
    }
}
