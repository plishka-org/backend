package org.plishka.backend.dto.admin.contacts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidHttpsUrl;

@Schema(description = "Admin social link create/update request.")
public record AdminContactsPageSocialLinkRequestDto(
        @Schema(description = "Social link name.", example = "Instagram")
        @NotBlank(message = "Social link name is required")
        @Size(max = 100, message = "Social link name must contain at most 100 characters")
        String name,

        @Schema(description = "Absolute HTTPS URL.", example = "https://instagram.com/plishka")
        @NotBlank(message = "Social link URL is required")
        @ValidHttpsUrl
        @Size(max = 512, message = "Social link URL must contain at most 512 characters")
        String url
) {
}
