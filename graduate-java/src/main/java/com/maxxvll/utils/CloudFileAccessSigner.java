package com.maxxvll.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class CloudFileAccessSigner {

    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    @Value("${app.cloud.access-sign-secret:${sa-token.jwt-secret-key}}")
    private String secret;

    private byte[] secretBytes;

    @PostConstruct
    public void init() {
        this.secretBytes = String.valueOf(secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8);
    }

    public String buildAccessPath(String accessMode, String objectName, String userId) {
        long expiresAt = Instant.now().plus(DEFAULT_TTL).toEpochMilli();
        String signature = sign(accessMode, objectName, userId, expiresAt);
        return UriComponentsBuilder.fromPath("/cloud/access/" + accessMode)
                .queryParam("object", objectName)
                .queryParam("uid", userId)
                .queryParam("exp", expiresAt)
                .queryParam("sig", signature)
                .build()
                .toUriString();
    }

    public boolean isValid(String accessMode, String objectName, String userId, Long expiresAt, String signature) {
        if (accessMode == null || accessMode.isBlank()
                || objectName == null || objectName.isBlank()
                || userId == null || userId.isBlank()
                || expiresAt == null
                || signature == null || signature.isBlank()) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            return false;
        }
        String expected = sign(accessMode, objectName, userId, expiresAt);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(String accessMode, String objectName, String userId, long expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            String payload = accessMode + '\n' + userId + '\n' + expiresAt + '\n' + objectName;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign cloud access url", e);
        }
    }
}
