package com.veggiepal.recipe.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.veggiepal.recipe.dto.request.IngredientRequest;
import com.veggiepal.recipe.dto.response.IngredientResponse;
import com.veggiepal.recipe.entity.Ingredient;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "normalizedName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    Ingredient toIngredient(IngredientRequest request);

    IngredientResponse toIngredientResponse(Ingredient ingredient);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "normalizedName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    void updateIngredient(
            @MappingTarget Ingredient ingredient,
            IngredientRequest request
    );
}
