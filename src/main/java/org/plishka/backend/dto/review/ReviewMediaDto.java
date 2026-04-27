package org.plishka.backend.dto.review;

public record ReviewMediaDto(
        Long id,
        String s3Key,
        String mediaType,
        Boolean isPrimary
) {
}
