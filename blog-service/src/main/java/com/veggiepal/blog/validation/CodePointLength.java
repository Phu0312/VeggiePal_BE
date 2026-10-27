package com.veggiepal.blog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CodePointLengthValidator.class)
public @interface CodePointLength {
    String message() default "BLOG_CONTENT_TOO_LONG";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int max();
}
