package com.veggiepal.blog.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * At most {@code max} whitespace-separated words. Null and blank values pass; pair it with
 * {@code @NotBlank} when the field is required.
 *
 * <p>The attribute is named {@code max} on purpose: {@code GlobalExceptionHandler} fills a
 * {@code {max}} placeholder in the {@code ErrorCode} message from it.
 */
@Documented
@Constraint(validatedBy = MaxWordsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxWords {

    String message() default "INVALID_KEY";

    int max();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
