package org.plishka.backend.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Presigned download URL request.")
public record PresignDownloadRequestDto(
        @Schema(
                description = "Opaque S3 object key for clients.",
                example = OpenApiExampleValues.PRODUCT_IMAGE_S3_KEY
        )
        @NotBlank(message = "S3 key is required")
        @Size(max = 512, message = "S3 key must not exceed 512 characters")
        String s3Key
) {
}
