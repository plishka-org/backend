package org.plishka.backend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LikePatternEscaperTest {
    @Test
    void containsPattern_ShouldEscapeLikeWildcardsAndEscapeCharacter() {
        assertEquals("%!%!_!!%", LikePatternEscaper.containsPattern("%_!"));
    }

    @Test
    void containsPattern_ShouldWrapPlainText() {
        assertEquals("%john%", LikePatternEscaper.containsPattern("john"));
    }
}
