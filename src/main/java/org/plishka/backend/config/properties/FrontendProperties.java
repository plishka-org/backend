package org.plishka.backend.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "frontend")
public record FrontendProperties(
        @NotBlank(message = "frontend.reset-password-url must not be blank")
        @URL(message = "frontend.reset-password-url must be a valid URL")
        String resetPasswordUrl
) {
}
