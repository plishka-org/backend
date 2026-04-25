package org.plishka.backend.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
        @NotBlank(message = "resend.api-key must not be blank")
        String apiKey,

        @NotBlank(message = "resend.from-email must not be blank")
        String fromEmail
) {
}
