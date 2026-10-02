package org.zayed.unirollweb.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SECRET_32_BYTES = "0123456789abcdef0123456789abcdef";

    @Test
    void acceptsSecretOfAtLeast32Bytes() {
        assertThatCode(() -> new JwtProperties(SECRET_32_BYTES, Duration.ofHours(1))).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortOrMissingSecret() {
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES.substring(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroOrNegativeExpiry() {
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(SECRET_32_BYTES, Duration.ofMinutes(-5)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
