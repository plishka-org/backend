package org.plishka.backend.ratelimit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RateLimitRequestBodyExtractor {
    private final ObjectMapper objectMapper;

    public Map<String, String> extract(byte[] body, Set<String> fields) {
        if (body.length == 0 || fields.isEmpty()) {
            return Map.of();
        }

        try {
            Map<?, ?> values = objectMapper.readValue(body, Map.class);
            Map<String, String> extractedValues = new HashMap<>();
            for (String field : fields) {
                Object value = values.get(field);
                if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                    extractedValues.put(field, stringValue);
                }
            }
            return extractedValues;
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }
}
