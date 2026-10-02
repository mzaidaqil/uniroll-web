package org.zayed.unirollweb.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiry) {

    public JwtProperties {
        // HS256 uses a 256-bit key; a shorter secret would make tokens easier to forge
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes");
        }
        if (expiry == null || expiry.isNegative() || expiry.isZero()) {
            throw new IllegalArgumentException("app.jwt.expiry must be positive");
        }
    }
}
