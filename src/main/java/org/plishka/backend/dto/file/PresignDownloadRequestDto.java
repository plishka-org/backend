package org.plishka.backend.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignDownloadRequestDto(
        @NotBlank(message = "S3 key is required")
        @Size(max = 512, message = "S3 key must not exceed 512 characters")
        String s3Key
) {
}
