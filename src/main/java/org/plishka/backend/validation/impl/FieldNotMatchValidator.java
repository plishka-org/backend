package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Objects;
import org.plishka.backend.validation.FieldNotMatch;
import org.springframework.beans.BeanWrapperImpl;

public class FieldNotMatchValidator implements ConstraintValidator<FieldNotMatch, Object> {
    private String firstFieldName;
    private String secondFieldName;
    private String message;

    @Override
    public void initialize(FieldNotMatch constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);
        Object firstValue = beanWrapper.getPropertyValue(firstFieldName);
        Object secondValue = beanWrapper.getPropertyValue(secondFieldName);

        if (firstValue == null || secondValue == null) {
            return true;
        }

        boolean notMatches = !Objects.equals(firstValue, secondValue);

        if (!notMatches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(secondFieldName)
                    .addConstraintViolation();
        }

        return notMatches;
    }
}
