package org.plishka.backend.dto.contacts;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Social link.")
public record SocialLinkDto(
        @Schema(description = "Social link id.", example = "1")
        Long socialLinkId,
        @Schema(description = "Social link name.", example = "Instagram")
        String name,
        @Schema(description = "Absolute HTTPS URL.", example = "https://instagram.com/plishka")
        String url
) {
}
