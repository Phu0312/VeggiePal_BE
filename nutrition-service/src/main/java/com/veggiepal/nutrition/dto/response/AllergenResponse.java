package com.veggiepal.nutrition.dto.response;

import com.veggiepal.nutrition.enums.AllergenCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllergenResponse {
    Long id;

    String code;

    String name;

    AllergenCategory category;
}
