package com.veggiepal.nutrition.service;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.AllergenMapper;
import com.veggiepal.nutrition.repository.AllergenRepository;
import com.veggiepal.nutrition.repository.UserAllergyRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AllergyService {

    // Sorted in Java: the order of a MySQL ENUM column depends on the column type
    private static final Comparator<Allergen> CATALOG_ORDER = Comparator
            .comparing(Allergen::getCategory)
            .thenComparing(Allergen::getName, Collator.getInstance(Locale.forLanguageTag("vi")));

    AllergenRepository allergenRepository;
    UserAllergyRepository userAllergyRepository;
    AllergenMapper allergenMapper;

    public List<AllergenResponse> getAllAllergens() {

        return toSortedResponses(allergenRepository.findAll());
    }

    public List<AllergenResponse> getMyAllergies(Long userId) {

        return toSortedResponses(userAllergyRepository.findAllergensByUserId(userId));
    }

    @Transactional
    public List<AllergenResponse> replaceAllergies(Long userId, UpdateAllergiesRequest request) {

        Set<Long> requestedIds = new HashSet<>(request.getAllergenIds());
        List<Allergen> requestedAllergens = allergenRepository.findAllById(requestedIds);

        if (requestedAllergens.size() != requestedIds.size()) {
            throw new AppException(ErrorCode.ALLERGEN_NOT_EXISTED);
        }

        List<UserAllergy> currentAllergies = userAllergyRepository.findByUserId(userId);
        Set<Long> currentIds = currentAllergies.stream()
                .map(userAllergy -> userAllergy.getAllergen().getId())
                .collect(Collectors.toSet());

        List<UserAllergy> removed = currentAllergies.stream()
                .filter(userAllergy -> !requestedIds.contains(userAllergy.getAllergen().getId()))
                .toList();

        List<UserAllergy> added = requestedAllergens.stream()
                .filter(allergen -> !currentIds.contains(allergen.getId()))
                .map(allergen -> UserAllergy.builder()
                        .userId(userId)
                        .allergen(allergen)
                        .build())
                .toList();

        userAllergyRepository.deleteAll(removed);
        userAllergyRepository.saveAll(added);

        return toSortedResponses(requestedAllergens);
    }

    private List<AllergenResponse> toSortedResponses(Collection<Allergen> allergens) {

        return allergens.stream()
                .sorted(CATALOG_ORDER)
                .map(allergenMapper::toAllergenResponse)
                .toList();
    }
}
