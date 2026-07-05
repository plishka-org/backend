package org.plishka.backend.dto.admin.contacts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidHttpsUrl;

public record AdminContactsPageSocialLinkRequestDto(
        @NotBlank(message = "Social link name is required")
        @Size(max = 100, message = "Social link name must contain at most 100 characters")
        String name,

        @NotBlank(message = "Social link URL is required")
        @ValidHttpsUrl
        @Size(max = 512, message = "Social link URL must contain at most 512 characters")
        String url
) {
}
