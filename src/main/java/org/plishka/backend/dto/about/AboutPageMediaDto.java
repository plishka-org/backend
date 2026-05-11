package org.plishka.backend.dto.about;

public record AboutPageMediaDto(
        Long id,
        String s3Key,
        String mediaType
) {
}
