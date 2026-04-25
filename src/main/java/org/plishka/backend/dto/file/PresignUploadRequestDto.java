package org.plishka.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;

public record PresignUploadRequestDto(
        @NotNull(message = "Target type is required")
        MediaTargetType targetType,

        @NotNull(message = "Target id is required")
        @Positive(message = "Target id must be greater than 0")
        Long targetId,

        @NotNull(message = "Media type is required")
        MediaType mediaType,

        @NotBlank(message = "Content type is required")
        @Size(max = 100, message = "Content type must not exceed 100 characters")
        String contentType,

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be greater than 0")
        Long sizeBytes,

        @NotBlank(message = "Original filename is required")
        @Size(max = 255, message = "Original filename must not exceed 255 characters")
        String originalFilename,

        @NotBlank(message = "Base64 SHA-256 checksum is required")
        @Size(min = 44, max = 44, message = "Base64 SHA-256 checksum must be 44 characters long")
        String checksumSha256Base64
) {
}
