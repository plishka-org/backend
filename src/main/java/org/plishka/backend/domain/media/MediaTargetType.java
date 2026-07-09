package org.plishka.backend.domain.media;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Schema(description = "Media attachment target type: PRODUCT, REVIEW, or ABOUT.")
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
