package org.plishka.backend.domain.media;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MediaTargetType {
    PRODUCT("products"),
    REVIEW("reviews"),
    ABOUT("about");

    private final String keySegment;

    public static Optional<MediaTargetType> fromKeySegment(String keySegment) {
        return Arrays.stream(values())
                .filter(targetType -> targetType.keySegment.equals(keySegment))
                .findFirst();
    }
}
