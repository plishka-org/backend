package org.plishka.backend.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {
    public static final int EMAIL_ACTION_TOKEN_BYTE_LENGTH = 32;
    public static final int EMAIL_ACTION_TOKEN_LENGTH = 43;

    public static final int REFRESH_TOKEN_BYTE_LENGTH = 64;
    public static final int REFRESH_TOKEN_LENGTH = 86;

    public static final String URL_SAFE_TOKEN_REGEX = "^[A-Za-z0-9_-]+$";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String generateEmailVerificationToken() {
        return generateAccountLinkToken();
    }

    public static String generateRefreshToken() {
        return generateUrlSafeToken(REFRESH_TOKEN_BYTE_LENGTH);
    }

    private static String generateAccountLinkToken() {
        return generateUrlSafeToken(EMAIL_ACTION_TOKEN_BYTE_LENGTH);
    }

    private static String generateUrlSafeToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
