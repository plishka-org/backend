package org.plishka.backend.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class UrlValidationUtil {
    private static final Set<String> GOOGLE_MAPS_ALLOWED_HOSTS = Set.of(
            "google.com",
            "googleusercontent.com",
            "goo.gl"
    );

    private UrlValidationUtil() {
    }

    public static boolean isValidAbsoluteHttpsUrl(String value) {
        return parseAndValidateAbsoluteHttpsUrl(value).isPresent();
    }

    public static boolean isValidGoogleMapsUrl(String value) {
        Optional<URI> validUri = parseAndValidateAbsoluteHttpsUrl(value);
        if (validUri.isEmpty()) {
            return false;
        }

        String host = validUri.get().getHost();
        return host != null && isAllowedGoogleMapsHost(host.toLowerCase(Locale.ROOT));
    }

    static Optional<URI> parseAndValidateAbsoluteHttpsUrl(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        if (containsWhitespaceOrControlCharacters(value)) {
            return Optional.empty();
        }

        try {
            URI uri = new URI(value.trim());

            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }

            String host = uri.getHost();
            if (host == null || host.isBlank() || "localhost".equalsIgnoreCase(host) || host.endsWith(".")) {
                return Optional.empty();
            }

            int lastDotIndex = host.lastIndexOf('.');
            if (lastDotIndex <= 0 || lastDotIndex == host.length() - 1) {
                return Optional.empty();
            }

            return Optional.of(uri);
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }

    private static boolean isAllowedGoogleMapsHost(String host) {
        String normalizedHost = host.startsWith("www.") ? host.substring(4) : host;

        return GOOGLE_MAPS_ALLOWED_HOSTS.stream()
                .anyMatch(allowedHost -> normalizedHost.equals(allowedHost)
                        || normalizedHost.endsWith("." + allowedHost));
    }

    private static boolean containsWhitespaceOrControlCharacters(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isWhitespace(character) || character < 0x20 || character == 0x7F) {
                return true;
            }
        }

        return false;
    }
}
