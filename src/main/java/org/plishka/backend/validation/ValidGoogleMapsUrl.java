package org.plishka.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.plishka.backend.validation.impl.GoogleMapsUrlValidator;

/**
 * Validates a Google Maps HTTPS URL using the project's custom rules.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code null}, an empty string, and a blank string are considered valid.</li>
 *   <li>The value must satisfy {@link ValidHttpsUrl} rules.</li>
 *   <li>The host must belong to an allowed Google Maps domain:
 * {@code google.com}, {@code googleusercontent.com}, or {@code goo.gl}.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = GoogleMapsUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGoogleMapsUrl {
    String message() default "Google Maps URL must be a valid HTTPS Google Maps link";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
