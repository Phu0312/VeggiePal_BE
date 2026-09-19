package com.veggiepal.nutrition.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAllergiesRequest {

    // [] clears all allergies; the list replaces the current one
    @NotNull(message = "ALLERGEN_IDS_REQUIRED")
    List<@NotNull(message = "ALLERGEN_NOT_EXISTED") Long> allergenIds;
}
