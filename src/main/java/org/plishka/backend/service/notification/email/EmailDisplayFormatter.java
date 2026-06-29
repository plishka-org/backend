package org.plishka.backend.service.notification.email;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class EmailDisplayFormatter {
    public static final String EMPTY_VALUE = "—";
    public static final String UNKNOWN_USER_ID = "видалено або невідомо";

    private static final Locale UKRAINIAN = Locale.of("uk", "UA");
    private static final ZoneId UKRAINE_ZONE = ZoneId.of("Europe/Kyiv");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", UKRAINIAN);

    public String formatDateTime(Instant instant) {
        return instant == null ? EMPTY_VALUE : DATE_TIME_FORMATTER.format(instant.atZone(UKRAINE_ZONE));
    }

    public String formatDurationHours(Duration duration) {
        if (duration == null) {
            return EMPTY_VALUE;
        }
        return duration.toHours() + " год.";
    }

    public String formatOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return EMPTY_VALUE;
        }
        return value;
    }

    public String formatPrice(Long price) {
        return price == null ? EMPTY_VALUE : price.toString();
    }

    public String formatUserId(Long userId) {
        return userId == null ? UNKNOWN_USER_ID : userId.toString();
    }
}
