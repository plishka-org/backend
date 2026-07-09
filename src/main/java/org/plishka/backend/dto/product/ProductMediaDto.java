package org.plishka.backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Product media.")
public record ProductMediaDto(
        @Schema(description = "Product media id.", example = "10")
        Long productMediaId,
        @Schema(
                description = "Opaque S3 object key for clients.",
                example = OpenApiExampleValues.PRODUCT_IMAGE_S3_KEY
        )
        String s3Key,
        @Schema(description = "Media type: IMAGE or VIDEO.", example = OpenApiExampleValues.IMAGE_MEDIA_TYPE)
        MediaType mediaType,
        @Schema(description = "Whether this media is primary.", example = "true")
        Boolean isPrimary,
        @Schema(description = "Display order.", example = "0")
        Integer displayOrder
) {
}
