package com.veggiepal.nutrition.mapper;

import org.mapstruct.Mapper;

import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;

@Mapper(componentModel = "spring")
public interface AllergenMapper {

    AllergenResponse toAllergenResponse(Allergen allergen);
}
