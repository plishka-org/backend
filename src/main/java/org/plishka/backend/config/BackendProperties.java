package org.plishka.backend.config;

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
        Duration refreshTokenExpiration
) {
    @AssertTrue(message = "backend.refresh-token-expiration must be greater than 0")
    public boolean isRefreshTokenExpirationPositive() {
        return refreshTokenExpiration == null || refreshTokenExpiration.isPositive();
    }
}
