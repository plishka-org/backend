package org.plishka.backend.domain.media;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MediaType {
    IMAGE("images"),
    VIDEO("videos");

    private final String keyFolder;

    public static Optional<MediaType> fromKeyFolder(String keyFolder) {
        return Arrays.stream(values())
                .filter(mediaType -> mediaType.keyFolder.equals(keyFolder))
                .findFirst();
    }
}
