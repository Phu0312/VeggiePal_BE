package com.veggiepal.blog.validation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxWordsValidator implements ConstraintValidator<MaxWords, String> {

    private static final Pattern WHITESPACE = Pattern.compile("\s+");

    private int max;

    public MaxWordsValidator() {
    }

    MaxWordsValidator(int max) {
        this.max = max;
    }

    @Override
    public void initialize(MaxWords annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return WHITESPACE.split(value.strip()).length <= max;
    }
}
