package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.NameValidator;

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
