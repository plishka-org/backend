package org.plishka.backend.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class RequestHashUtil {
    private static final char LENGTH_VALUE_SEPARATOR = ':';

    private RequestHashUtil() {
    }

    public static String hash(String... fields) {
        return TokenHashUtil.sha256(canonicalize(fields));
    }

    private static String canonicalize(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String normalizedValue = Objects.toString(value, "");
            int byteLength = normalizedValue.getBytes(StandardCharsets.UTF_8).length;
            builder.append(byteLength)
                    .append(LENGTH_VALUE_SEPARATOR)
                    .append(normalizedValue);
        }
        return builder.toString();
    }
}
