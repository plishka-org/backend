package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.EmailActionTokenValidator;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = EmailActionTokenValidator.class)
public @interface EmailActionToken {
    String message() default "Verification token has invalid format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
