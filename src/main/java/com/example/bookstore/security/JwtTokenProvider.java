package com.example.bookstore.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final String jwtSecret;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.jwtSecret = jwtSecret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(principal);
    }

    public String generateToken(UserPrincipal principal) {
        long now = Instant.now().getEpochSecond();
        long expiration = now + expirationMs / 1000;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + escapeJson(principal.getUsername()) + "\","
                + "\"userId\":" + principal.getUserId() + ","
                + "\"role\":\"" + escapeJson(principal.getRole()) + "\","
                + "\"iat\":" + now + ","
                + "\"exp\":" + expiration
                + "}";

        String encodedHeader = encode(header);
        String encodedPayload = encode(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public boolean validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return false;
        }

        Long expiration = extractLongClaim(token, "exp");
        return expiration != null && expiration > Instant.now().getEpochSecond();
    }

    public String getUsernameFromToken(String token) {
        return extractStringClaim(token, "sub");
    }

    public String getRoleFromToken(String token) {
        return extractStringClaim(token, "role");
    }

    private String encode(String value) {
        return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot sign JWT token", exception);
        }
    }

    private String extractStringClaim(String token, String claimName) {
        String payload = decodePayload(token);
        Matcher matcher = Pattern.compile("\"" + claimName + "\"\\s*:\\s*\"([^\"]*)\"").matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Long extractLongClaim(String token, String claimName) {
        String payload = decodePayload(token);
        Matcher matcher = Pattern.compile("\"" + claimName + "\"\\s*:\\s*(\\d+)").matcher(payload);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return "";
        }
        return new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestHelper.constantTimeEquals(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
