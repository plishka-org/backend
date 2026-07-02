package org.plishka.backend.dto.admin.home;

public record AdminHomePageAdvantageDto(
        Long advantageId,
        String title,
        String description,
        String iconS3Key,
        Integer displayOrder
) {
}
