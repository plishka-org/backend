package org.plishka.backend.dto.admin.about;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.domain.media.MediaType;

@Schema(description = "Admin about page media.")
public record AdminAboutPageMediaDto(
        @Schema(description = "Media id.", example = "10")
        Long mediaId,
        @Schema(
                description = "Opaque S3 object key for clients.",
                example = "about/1/images/2026/07/550e8400-e29b-41d4-a716-446655440000.webp"
        )
        String s3Key,
        @Schema(description = "Media type: IMAGE or VIDEO.", example = "IMAGE")
        MediaType mediaType,
        @Schema(description = "Display order.", example = "0")
        Integer displayOrder
) {
}
