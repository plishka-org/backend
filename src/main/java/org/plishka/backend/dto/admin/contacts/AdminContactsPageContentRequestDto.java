package org.plishka.backend.dto.admin.contacts;

import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;
import org.plishka.backend.validation.ValidPhone;

public record AdminContactsPageContentRequestDto(
        @ValidPhone
        @Size(max = 20, message = "Phone number must contain at most 20 characters")
        String phoneNumber,

        @ValidEmail
        @Size(max = 128, message = "Email must contain at most 128 characters")
        String email,

        @Size(max = 255, message = "Address must contain at most 255 characters")
        String address,

        @Size(max = 512, message = "Google Maps URL must contain at most 512 characters")
        String googleMapsUrl
) {
}
