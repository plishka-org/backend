package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.PasswordValidator;

/**
 * Validates a password using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>Required value and total length must be enforced separately, for example with
 *   {@code @NotBlank} and {@code @Size}.</li>
 *   <li>The value must contain at least one lowercase Latin letter, one uppercase Latin letter,
 *   and one digit.</li>
 *   <li>Only ASCII letters, digits, and these special characters are allowed:
 *   {@code ~ ! ? @ # $ % ^ & * _ - + ( ) [ ] { } > < / \ | " ' . , :}.</li>
 *   <li>Whitespace and any characters outside this set are not allowed.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must contain at least: one uppercase letter, one lowercase letter and one digit."
            + " Allowed special characters: ~ ! ? @ # $ % ^ & * _ - + ( ) [ ] { } > < / \\ | \" ' . , :";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
