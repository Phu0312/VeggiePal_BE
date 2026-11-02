package com.veggiepal.recipe.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.recipe.dto.request.IngredientRequest;
import com.veggiepal.recipe.dto.response.IngredientResponse;
import com.veggiepal.recipe.dto.response.PageResponse;
import com.veggiepal.recipe.entity.Ingredient;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;
import com.veggiepal.recipe.mapper.IngredientMapper;
import com.veggiepal.recipe.repository.IngredientRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IngredientService {

    IngredientRepository ingredientRepository;
    IngredientMapper ingredientMapper;

    @Transactional
    public IngredientResponse createIngredient(IngredientRequest request) {

        String name = request.getName().trim();
        String normalizedName = normalizeName(name);

        if (ingredientRepository.existsByNormalizedName(normalizedName)) {
            throw new AppException(
                    ErrorCode.INGREDIENT_NAME_DUPLICATED
            );
        }

        Ingredient ingredient =
                ingredientMapper.toIngredient(request);

        ingredient.setName(name);
        ingredient.setNormalizedName(normalizedName);

        if (ingredient.getVegan() == null) {
            ingredient.setVegan(true);
        }

        if (ingredient.getAllergen() == null) {
            ingredient.setAllergen(false);
        }

        if (ingredient.getActive() == null) {
            ingredient.setActive(true);
        }

        if (ingredient.getDescription() != null) {
            ingredient.setDescription(
                    trimToNull(ingredient.getDescription())
            );
        }

        Ingredient savedIngredient =
                ingredientRepository.save(ingredient);

        return ingredientMapper
                .toIngredientResponse(savedIngredient);
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredient(Long id) {

        Ingredient ingredient = findById(id);

        return ingredientMapper
                .toIngredientResponse(ingredient);
    }

    @Transactional(readOnly = true)
    public PageResponse<IngredientResponse> getIngredients(
            String keyword,
            Boolean vegan,
            Boolean allergen,
            int page,
            int size
    ) {

        int safePage = Math.max(page, 0);

        int safeSize =
                Math.min(
                        Math.max(size, 1),
                        100
                );

        String safeKeyword =
                normalizeKeyword(keyword);

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(
                                Sort.Direction.ASC,
                                "name"
                        )
                );

        Page<Ingredient> ingredientPage =
                ingredientRepository.search(
                        safeKeyword,
                        vegan,
                        allergen,
                        pageable
                );

        return PageResponse
                .<IngredientResponse>builder()
                .items(
                        ingredientPage
                                .getContent()
                                .stream()
                                .map(
                                        ingredientMapper
                                                ::toIngredientResponse
                                )
                                .toList()
                )
                .page(
                        ingredientPage.getNumber()
                )
                .size(
                        ingredientPage.getSize()
                )
                .totalElements(
                        ingredientPage.getTotalElements()
                )
                .totalPages(
                        ingredientPage.getTotalPages()
                )
                .build();
    }

    @Transactional
    public IngredientResponse updateIngredient(
            Long id,
            IngredientRequest request
    ) {

        Ingredient ingredient = findById(id);

        String name = request.getName().trim();

        String normalizedName =
                normalizeName(name);

        if (ingredientRepository
                .existsByNormalizedNameAndIdNot(
                        normalizedName,
                        id
                )) {

            throw new AppException(
                    ErrorCode.INGREDIENT_NAME_DUPLICATED
            );
        }

        ingredientMapper.updateIngredient(
                ingredient,
                request
        );

        ingredient.setName(name);
        ingredient.setNormalizedName(
                normalizedName
        );

        if (ingredient.getDescription() != null) {
            ingredient.setDescription(
                    trimToNull(
                            ingredient.getDescription()
                    )
            );
        }

        Ingredient savedIngredient =
                ingredientRepository.save(ingredient);

        return ingredientMapper
                .toIngredientResponse(savedIngredient);
    }

    @Transactional
    public void deleteIngredient(Long id) {

        Ingredient ingredient = findById(id);

        ingredientRepository.delete(ingredient);
    }

    private Ingredient findById(Long id) {

        return ingredientRepository
                .findById(id)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode
                                        .INGREDIENT_NOT_EXISTED
                        )
                );
    }

    private String normalizeName(String value) {

        String normalized =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized.replaceAll(
                        "\\p{M}",
                        ""
                );

        return normalized
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String normalizeKeyword(
            String keyword
    ) {

        if (keyword == null) {
            return null;
        }

        String value =
                keyword.trim();

        return value.isBlank()
                ? null
                : value;
    }

    private String trimToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isBlank()
                ? null
                : trimmed;
    }
}