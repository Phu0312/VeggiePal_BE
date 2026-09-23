package com.veggiepal.blog.dto.response;

import java.util.List;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {

    Long id;

    Long parentId;

    CategoryType type;

    String name;

    Short displayOrder;

    Boolean active;

    List<CategoryResponse> children;
}
