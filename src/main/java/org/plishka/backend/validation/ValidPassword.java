package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.PasswordValidator;


@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default """
            Password must contain at least:
            - one uppercase letter
            - one lowercase letter
            - one digit
            Allowed special characters: ~ ! ? @ # $ % ^ & * _ - + ( ) [ ] { } > < / \\ | " ' . , :
            """;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
