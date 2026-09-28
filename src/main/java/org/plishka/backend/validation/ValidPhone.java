package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.PhoneValidator;

/**
 * Validates a phone number using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>The value must match the exact format {@code +380XXXXXXXXX}.</li>
 *   <li>After the {@code +380} prefix, exactly 9 digits are required.</li>
 *   <li>Spaces, dashes, parentheses, and any other extra characters are not allowed.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Phone must match +380XXXXXXXXX format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
