package org.plishka.backend.dto.admin.contacts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;
import org.plishka.backend.validation.ValidGoogleMapsUrl;
import org.plishka.backend.validation.ValidPhone;

@Schema(description = "Admin contacts page content update request. All fields are optional and nullable.")
public record AdminContactsPageContentRequestDto(
        @Schema(description = "Optional phone number.", example = "+380501234567", nullable = true)
        @ValidPhone
        @Size(max = 20, message = "Phone number must contain at most 20 characters")
        String phoneNumber,

        @Schema(description = "Optional email address.", example = "support@example.com", nullable = true)
        @ValidEmail
        @Size(max = 128, message = "Email must contain at most 128 characters")
        String email,

        @Schema(description = "Optional physical address.", example = "Kyiv, Ukraine", nullable = true)
        @Size(max = 255, message = "Address must contain at most 255 characters")
        String address,

        @Schema(description = "Optional Google Maps HTTPS URL.", example = "https://maps.google.com/...", nullable = true)
        @ValidGoogleMapsUrl
        @Size(max = 512, message = "Google Maps URL must contain at most 512 characters")
        String googleMapsUrl
) {
}
