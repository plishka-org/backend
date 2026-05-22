package org.plishka.backend.dto.home;

public record HomePageAdvantageDto(
        Long homePageAdvantageId,
        String title,
        String description,
        String iconS3Key
) {
}
