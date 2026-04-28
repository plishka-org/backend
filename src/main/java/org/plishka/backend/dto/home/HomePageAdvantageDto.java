package org.plishka.backend.dto.home;

public record HomePageAdvantageDto(
        Long id,
        String title,
        String description,
        String iconS3Key
) {
}
