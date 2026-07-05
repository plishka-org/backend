package org.plishka.backend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Rate-limit settings loaded by Spring Boot.
 *
 * <p>{@code application.yaml} keeps the small runtime settings: {@code enabled} and cache size.
 * The policy matrix is kept separately in {@code rate-limit-policies.yaml}.
 *
 * <p>YAML policies are the normal source of limits. If a known policy is missing from YAML,
 * {@link org.plishka.backend.ratelimit.RateLimitPolicy} supplies fallback limits from code.
 *
 * <p>Policy names are map keys, for example {@code auth-login-ip}. Unknown keys are not used, so a
 * typo in YAML can silently fall back to the enum default.
 */
@Validated
@ConfigurationProperties(prefix = "backend.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true")
        boolean enabled,

        @NotNull(message = "backend.rate-limit.cache must not be null")
        @Valid
        @DefaultValue
        Cache cache,

        @Valid
        Map<String, Policy> policies
) {
    public RateLimitProperties {
        if (cache == null) {
            cache = new Cache(Duration.ofHours(24), 100_000L);
        }
        policies = policies == null ? Map.of() : Map.copyOf(policies);
    }

    public record Cache(
            @NotNull(message = "backend.rate-limit.cache.expire-after-access must not be null")
            @DurationMin(seconds = 1, message = "backend.rate-limit.cache.expire-after-access must be greater than 0")
            @DefaultValue("24h")
            Duration expireAfterAccess,

            @Min(value = 1, message = "backend.rate-limit.cache.maximum-size must be greater than 0")
            @DefaultValue("100000")
            long maximumSize
    ) {
        public Cache {
            if (expireAfterAccess == null) {
                expireAfterAccess = Duration.ofHours(24);
            }
        }
    }

    public record Policy(
            @NotEmpty(message = "backend.rate-limit policy bandwidths must not be empty")
            @Valid
            List<Bandwidth> bandwidths
    ) {
        public Policy {
            bandwidths = bandwidths == null ? List.of() : List.copyOf(bandwidths);
        }
    }

    public record Bandwidth(
            @Min(value = 1, message = "backend.rate-limit bandwidth capacity must be greater than 0")
            long capacity,

            @NotNull(message = "backend.rate-limit bandwidth period must not be null")
            @DurationMin(seconds = 1, message = "backend.rate-limit bandwidth period must be greater than 0")
            Duration period
    ) {
    }
}
