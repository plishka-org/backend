package org.plishka.backend.config.properties;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {
    public List<String> normalizedAllowedOrigins() {
        if (allowedOrigins == null) {
            return List.of();
        }

        return allowedOrigins.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(CorsProperties::removeTrailingSlashes)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList();
    }

    private static String removeTrailingSlashes(String origin) {
        String normalizedOrigin = origin;
        while (normalizedOrigin.endsWith("/")) {
            normalizedOrigin = normalizedOrigin.substring(0, normalizedOrigin.length() - 1);
        }
        return normalizedOrigin;
    }
}
