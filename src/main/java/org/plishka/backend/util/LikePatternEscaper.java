package org.plishka.backend.util;

public final class LikePatternEscaper {
    public static final char ESCAPE_CHARACTER = '!';

    private static final String ESCAPE = String.valueOf(ESCAPE_CHARACTER);
    private static final String SQL_ANY_CHAR_WILDCARD = "_";
    private static final String SQL_ANY_SEQUENCE_WILDCARD = "%";

    private LikePatternEscaper() {
    }

    public static String containsPattern(String value) {
        return SQL_ANY_SEQUENCE_WILDCARD + escape(value) + SQL_ANY_SEQUENCE_WILDCARD;
    }

    private static String escape(String value) {
        return value
                .replace(ESCAPE, ESCAPE + ESCAPE)
                .replace(SQL_ANY_SEQUENCE_WILDCARD, ESCAPE + SQL_ANY_SEQUENCE_WILDCARD)
                .replace(SQL_ANY_CHAR_WILDCARD, ESCAPE + SQL_ANY_CHAR_WILDCARD);
    }
}
