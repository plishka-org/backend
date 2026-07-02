package org.plishka.backend.dto.admin.about;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAboutPageContentRequestDto(
        @NotBlank(message = "History title is required")
        @Size(max = 255, message = "History title must contain at most 255 characters")
        String historyTitle,

        @NotBlank(message = "History text is required")
        @Size(max = 10000, message = "History text must contain at most 10000 characters")
        String historyText,

        @NotBlank(message = "Current title is required")
        @Size(max = 255, message = "Current title must contain at most 255 characters")
        String currentTitle,

        @NotBlank(message = "Current text is required")
        @Size(max = 10000, message = "Current text must contain at most 10000 characters")
        String currentText
) {
}
