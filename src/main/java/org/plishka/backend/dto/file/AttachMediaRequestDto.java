package org.plishka.backend.dto.file;

import jakarta.validation.constraints.NotBlank;

public record AttachMediaRequestDto(
        @NotBlank(message = "S3 key is required")
        String s3Key
) {
}
