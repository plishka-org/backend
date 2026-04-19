package org.plishka.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "backend")
public record BackendProperties(
        @NotBlank(message = "backend.base-url must not be blank")
        @URL(message = "backend.base-url must be a valid URL")
        String baseUrl,

        @NotNull(message = "backend.refresh-token-expiration must not be null")
        Duration refreshTokenExpiration,

        @NotNull(message = "backend.auth must not be null")
        @Valid
        Auth auth,

        @NotNull(message = "backend.cleanup must not be null")
        @Valid
        Cleanup cleanup
) {
    @AssertTrue(message = "backend.refresh-token-expiration must be greater than 0")
    public boolean isRefreshTokenExpirationPositive() {
        return refreshTokenExpiration == null || refreshTokenExpiration.isPositive();
    }

    public record Auth(
            @NotNull(message = "backend.auth.email-verification-token-ttl must not be null")
            Duration emailVerificationTokenTtl,

            @NotNull(message = "backend.auth.password-reset-token-ttl must not be null")
            Duration passwordResetTokenTtl
    ) {
        @AssertTrue(message = "backend.auth.email-verification-token-ttl must be greater than 0")
        public boolean isEmailVerificationTokenTtlPositive() {
            return emailVerificationTokenTtl == null || emailVerificationTokenTtl.isPositive();
        }

        @AssertTrue(message = "backend.auth.password-reset-token-ttl must be greater than 0")
        public boolean isPasswordResetTokenTtlPositive() {
            return passwordResetTokenTtl == null || passwordResetTokenTtl.isPositive();
        }

        @AssertTrue(message = "backend.auth.email-verification-token-ttl must be a whole number of hours")
        public boolean isEmailVerificationTokenTtlWholeHours() {
            return emailVerificationTokenTtl == null
                    || emailVerificationTokenTtl.equals(Duration.ofHours(emailVerificationTokenTtl.toHours()));
        }

        @AssertTrue(message = "backend.auth.password-reset-token-ttl must be a whole number of hours")
        public boolean isPasswordResetTokenTtlWholeHours() {
            return passwordResetTokenTtl == null
                    || passwordResetTokenTtl.equals(Duration.ofHours(passwordResetTokenTtl.toHours()));
        }
    }

    public record Cleanup(
            @NotNull(message = "backend.cleanup.unverified-user-ttl must not be null")
            Duration unverifiedUserTtl
    ) {
        @AssertTrue(message = "backend.cleanup.unverified-user-ttl must be greater than 0")
        public boolean isUnverifiedUserTtlPositive() {
            return unverifiedUserTtl == null || unverifiedUserTtl.isPositive();
        }
    }
}
