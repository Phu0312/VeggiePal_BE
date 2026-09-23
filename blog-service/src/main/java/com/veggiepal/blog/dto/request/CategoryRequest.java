package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {

    @NotBlank(message = "CATEGORY_NAME_REQUIRED")
    @Size(max = 100, message = "CATEGORY_NAME_REQUIRED")
    String name;

    /** Required for a root category; a child inherits its parent's type. */
    CategoryType type;

    /** Null creates a root category. Ignored on update. */
    Long parentId;

    Short displayOrder;

    Boolean active;
}
