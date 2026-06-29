package org.plishka.backend.service.notification;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.service.notification.email.EmailDisplayFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDisplayFormatterTest {
    private EmailDisplayFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EmailDisplayFormatter();
    }

    @Test
    void formatDateTime_ShouldUseUkrainianLocaleAndKyivZone() {
        String formatted = formatter.formatDateTime(Instant.parse("2026-06-19T12:00:00Z"));

        assertTrue(formatted.contains("2026"));
        assertTrue(formatted.contains("червня"));
        assertTrue(formatted.contains("15:00"));
    }

    @Test
    void formatDateTime_ShouldReturnEmptyValueForNullInstant() {
        assertEquals(EmailDisplayFormatter.EMPTY_VALUE, formatter.formatDateTime(null));
    }

    @Test
    void formatDurationHours_ShouldReturnReadableUkrainianLabel() {
        assertEquals("24 год.", formatter.formatDurationHours(Duration.ofHours(24)));
    }

    @Test
    void formatDurationHours_ShouldReturnEmptyValueForNullDuration() {
        assertEquals(EmailDisplayFormatter.EMPTY_VALUE, formatter.formatDurationHours(null));
    }

    @Test
    void formatOptionalText_ShouldReturnEmptyValueForBlankInput() {
        assertEquals(EmailDisplayFormatter.EMPTY_VALUE, formatter.formatOptionalText(null));
        assertEquals(EmailDisplayFormatter.EMPTY_VALUE, formatter.formatOptionalText("  "));
        assertEquals("коментар", formatter.formatOptionalText("коментар"));
    }

    @Test
    void formatPrice_ShouldReturnEmptyValueForNullPrice() {
        assertEquals(EmailDisplayFormatter.EMPTY_VALUE, formatter.formatPrice(null));
        assertEquals("900", formatter.formatPrice(900L));
    }

    @Test
    void formatUserId_ShouldReturnUnknownLabelWhenMissing() {
        assertEquals(EmailDisplayFormatter.UNKNOWN_USER_ID, formatter.formatUserId(null));
        assertEquals("42", formatter.formatUserId(42L));
    }
}
