package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.EmailValidator;

/**
 * Validates an email address using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>Required value and total length must be enforced separately, for example with
 *   {@code @NotBlank} and {@code @Size(min = 5, max = 128)}.</li>
 *   <li>The value must contain exactly one {@code @} character.</li>
 *   <li>The local part must be non-empty, at most 64 characters long, must not start or end
 *   with a dot, must not contain consecutive dots, and may contain only Latin letters, digits,
 *   and {@code . _ % + -}.</li>
 *   <li>The domain part must be non-empty and contain at least one dot.</li>
 *   <li>Each non-TLD domain label must be 1 to 63 characters long, may contain only Latin
 *   letters, digits, and hyphens, and must not start or end with a hyphen.</li>
 *   <li>The top-level domain must be 2 to 63 characters long and contain only Latin letters.</li>
 *   <li>IP literals, quoted local parts, and other uncommon RFC-compatible formats are not
 *   supported.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {
    String message() default "Email must be a valid email address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
