package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.OrderNoteValidator;

/**
 * Validates a free-form order note using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid
 *   (the note is optional).</li>
 *   <li>Total length must be enforced separately, for example with {@code @Size}.</li>
 *   <li>The value must contain at least one Latin or Cyrillic letter, so that
 *   meaningless input such as {@code "!!!!!!!!!!"} or {@code "**"} is rejected.</li>
 *   <li>The value may contain only letters, digits, whitespace, and common
 *   punctuation.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = OrderNoteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrderNote {
    String message() default "Notes must contain meaningful text and may include only "
            + "letters, digits, spaces, and common punctuation.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
