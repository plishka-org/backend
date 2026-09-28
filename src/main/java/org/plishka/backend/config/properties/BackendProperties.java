package org.plishka.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "backend")
public record BackendProperties(
        @NotBlank(message = "backend.base-url must not be blank")
        @URL(message = "backend.base-url must be a valid URL")
        String baseUrl,

        @NotNull(message = "backend.refresh-token-expiration must not be null")
        @DurationMin(nanos = 1, message = "backend.refresh-token-expiration must be greater than 0")
        Duration refreshTokenExpiration,

        @NotNull(message = "backend.auth must not be null")
        @Valid
        Auth auth,

        @NotNull(message = "backend.cleanup must not be null")
        @Valid
        Cleanup cleanup
) {
    public record Auth(
            @NotNull(message = "backend.auth.email-verification-token-ttl must not be null")
            @DurationMin(nanos = 1, message = "backend.auth.email-verification-token-ttl must be greater than 0")
            Duration emailVerificationTokenTtl,

            @NotNull(message = "backend.auth.password-reset-token-ttl must not be null")
            @DurationMin(nanos = 1, message = "backend.auth.password-reset-token-ttl must be greater than 0")
            Duration passwordResetTokenTtl
    ) {
    }

    public record Cleanup(
            @NotNull(message = "backend.cleanup.unverified-user-ttl must not be null")
            @DurationMin(nanos = 1, message = "backend.cleanup.unverified-user-ttl must be greater than 0")
            Duration unverifiedUserTtl
    ) {
    }
}
