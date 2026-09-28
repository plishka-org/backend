package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.NameValidator;

/**
 * Validates a personal name using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>Required value and total length must be enforced separately, for example with
 *   {@code @NotBlank} and {@code @Size}.</li>
 *   <li>The value may contain only Latin or Cyrillic letters.</li>
 *   <li>Words may be separated only by a single space, hyphen, or apostrophe.</li>
 *   <li>The value must start and end with a letter.</li>
 *   <li>Repeated separators and separators without letters on both sides are not allowed.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = NameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {
    String message() default "Name may contain only Latin or "
            + "Cyrillic letters, spaces, hyphens, and apostrophes (' ’ ʼ).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
