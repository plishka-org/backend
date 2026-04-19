package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.ValidEmail;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPassword;
import org.plishka.backend.validation.ValidPhone;

@FieldMatch(first = "password", second = "confirmPassword",
        message = "Password and confirm password must match exactly")
public record RegisterRequestDto(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @NotBlank(message = "Email is required")
        @ValidEmail
        @Size(min = 5, max = 128, message = "Email must be between 5 and 128 characters long")
        String email,

        @ValidPhone
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        @ValidPassword
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 64, message =
                "Confirm password must be between 8 and 64 characters long")
        String confirmPassword
) {
}
