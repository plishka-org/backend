package org.plishka.backend.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Presigned upload URL request.")
public record PresignUploadRequestDto(
        @Schema(description = "Upload target type: PRODUCT, REVIEW, or ABOUT.",
                example = OpenApiExampleValues.PRODUCT_TARGET_TYPE)
        @NotNull(message = "Target type is required")
        MediaTargetType targetType,

        @Schema(description = "Target entity id.", example = "123")
        @NotNull(message = "Target id is required")
        @Positive(message = "Target id must be greater than 0")
        Long targetId,

        @Schema(description = "Media type: IMAGE or VIDEO.", example = OpenApiExampleValues.IMAGE_MEDIA_TYPE)
        @NotNull(message = "Media type is required")
        MediaType mediaType,

        @Schema(
                description = "Upload content type. Allowed values: image/jpeg, image/png, image/webp, "
                        + "video/mp4, video/webm.",
                example = OpenApiExampleValues.IMAGE_WEBP_CONTENT_TYPE
        )
        @NotBlank(message = "Content type is required")
        @Size(max = 100, message = "Content type must not exceed 100 characters")
        String contentType,

        @Schema(description = "File size in bytes. Maximum file sizes are configured by server.", example = "1048576")
        @NotNull(message = "File size is required")
        @Positive(message = "File size must be greater than 0")
        Long sizeBytes,

        @Schema(description = "Original filename with one allowed extension.",
                example = OpenApiExampleValues.IMAGE_WEBP_FILENAME)
        @NotBlank(message = "Original filename is required")
        @Size(max = 255, message = "Original filename must not exceed 255 characters")
        String originalFilename,

        @Schema(
                description = "Base64-encoded SHA-256 checksum, exactly 44 characters.",
                example = OpenApiExampleValues.CHECKSUM_SHA256_BASE64
        )
        @NotBlank(message = "Base64 SHA-256 checksum is required")
        @Size(min = 44, max = 44, message = "Base64 SHA-256 checksum must be 44 characters long")
        String checksumSha256Base64
) {
}
