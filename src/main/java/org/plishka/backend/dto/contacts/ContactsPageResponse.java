package org.plishka.backend.dto.contacts;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Contacts page response.")
public record ContactsPageResponse(
        @Schema(description = "Optional phone number.", example = "+380501234567", nullable = true)
        String phoneNumber,
        @Schema(description = "Optional email address.", example = "support@example.com", nullable = true)
        String email,
        @Schema(description = "Optional physical address.", example = "Kyiv, Ukraine", nullable = true)
        String address,
        @Schema(description = "Optional Google Maps HTTPS URL.", example = "https://maps.google.com/...", nullable = true)
        String googleMapsUrl,
        @Schema(description = "Social links.")
        List<SocialLinkDto> socialLinks
) {
}
