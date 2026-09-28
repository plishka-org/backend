package org.plishka.backend.dto.about;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.domain.media.MediaType;

@Schema(description = "About page media.")
public record AboutPageMediaDto(
        @Schema(description = "About page media id.", example = "10")
        Long aboutPageMediaId,
        @Schema(
                description = "Opaque S3 object key for clients.",
                example = "about/1/images/2026/07/550e8400-e29b-41d4-a716-446655440000.webp"
        )
        String s3Key,
        @Schema(description = "Media type: IMAGE or VIDEO.", example = "IMAGE")
        MediaType mediaType
) {
}
