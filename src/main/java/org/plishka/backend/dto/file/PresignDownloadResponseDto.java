package org.plishka.backend.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Builder
@Schema(description = "Presigned download URL response.")
public record PresignDownloadResponseDto(
        @Schema(
                description = "Opaque S3 object key for clients.",
                example = OpenApiExampleValues.PRODUCT_IMAGE_S3_KEY
        )
        String s3Key,
        @Schema(
                description = "Temporary presigned download URL. Reuse it until expiresAt instead of presigning again.",
                example = OpenApiExampleValues.PRESIGNED_STORAGE_URL
        )
        String downloadUrl,
        @Schema(description = "HTTP method to use with the presigned URL.", example = "GET")
        String method,
        @Schema(
                description = "URL expiration timestamp in UTC ISO-8601 format.",
                example = OpenApiExampleValues.PRESIGNED_URL_EXPIRES_AT
        )
        Instant expiresAt
) {
}
