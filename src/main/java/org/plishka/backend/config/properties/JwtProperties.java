package org.plishka.backend.config.properties;

import io.jsonwebtoken.io.Decoders;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "jwt.secret must not be blank")
        String secret,

        @NotNull(message = "jwt.expiration must not be null")
        @DurationMin(nanos = 1, message = "jwt.expiration must be greater than 0")
        Duration expiration
) {
    private static final int MIN_HMAC_SHA_KEY_BYTES = 32;

    @AssertTrue(message = "jwt.secret must be valid Base64 and decode to at least 32 bytes")
    public boolean isSecretValidForHmacSha() {
        if (!StringUtils.hasText(secret)) {
            return true;
        }

        try {
            return Decoders.BASE64.decode(secret).length >= MIN_HMAC_SHA_KEY_BYTES;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
