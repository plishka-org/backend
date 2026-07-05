package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.HttpsUrlValidator;

/**
 * Validates an absolute HTTPS URL using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>Required value must be enforced separately, for example with {@code @NotBlank}.</li>
 *   <li>The scheme must be strictly {@code https}.</li>
 *   <li>The host must be present and must not be {@code localhost}.</li>
 *   <li>The host must contain at least one dot with a non-empty domain suffix.</li>
 *   <li>Values with whitespace or control characters are rejected.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = HttpsUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidHttpsUrl {
    String message() default "URL must be a valid absolute HTTPS URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
