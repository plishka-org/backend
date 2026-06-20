package org.plishka.backend.util;

import java.text.Normalizer;
import java.util.Locale;

public final class UserInputNormalizer {
    private UserInputNormalizer() {
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeName(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC).trim();
    }

    public static String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }
}
