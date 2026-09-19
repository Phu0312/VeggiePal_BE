package com.veggiepal.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;
import com.veggiepal.nutrition.enums.AllergenCategory;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.AllergenMapper;
import com.veggiepal.nutrition.repository.AllergenRepository;
import com.veggiepal.nutrition.repository.UserAllergyRepository;

@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {

    static final Long USER_ID = 7L;

    static final Allergen GLUTEN = allergen(1L, "GLUTEN", "Gluten (lúa mì, lúa mạch)", AllergenCategory.GRAIN);

    static final Allergen PEANUT = allergen(4L, "PEANUT", "Đậu phộng", AllergenCategory.LEGUME);

    static final Allergen SOY = allergen(5L, "SOY", "Đậu nành", AllergenCategory.LEGUME);

    static final Allergen MANGO = allergen(20L, "MANGO", "Xoài", AllergenCategory.FRUIT);

    @Mock
    AllergenRepository allergenRepository;

    @Mock
    UserAllergyRepository userAllergyRepository;

    @Spy
    AllergenMapper allergenMapper = Mappers.getMapper(AllergenMapper.class);

    @InjectMocks
    AllergyService allergyService;

    @Captor
    ArgumentCaptor<Iterable<UserAllergy>> userAllergiesCaptor;

    @Test
    void getAllAllergens_sortedByCategoryThenVietnameseName() {
        when(allergenRepository.findAll()).thenReturn(List.of(MANGO, PEANUT, SOY, GLUTEN));

        List<AllergenResponse> allergens = allergyService.getAllAllergens();

        assertThat(allergens).extracting(AllergenResponse::getCode)
                .containsExactly("GLUTEN", "SOY", "PEANUT", "MANGO");
    }

    @Test
    void getMyAllergies_returnsSortedAllergensOfUser() {
        when(userAllergyRepository.findAllergensByUserId(USER_ID)).thenReturn(List.of(MANGO, GLUTEN));

        assertThat(allergyService.getMyAllergies(USER_ID)).extracting(AllergenResponse::getCode)
                .containsExactly("GLUTEN", "MANGO");
    }

    @Test
    void replaceAllergies_removesAndAddsOnlyTheDifference() {
        UserAllergy peanutAllergy = userAllergy(11L, PEANUT);
        UserAllergy soyAllergy = userAllergy(12L, SOY);
        when(allergenRepository.findAllById(Set.of(5L, 20L))).thenReturn(List.of(SOY, MANGO));
        when(userAllergyRepository.findByUserId(USER_ID)).thenReturn(List.of(peanutAllergy, soyAllergy));

        List<AllergenResponse> result = allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of(5L, 20L, 20L)));

        verify(userAllergyRepository).deleteAll(List.of(peanutAllergy));
        verify(userAllergyRepository).saveAll(userAllergiesCaptor.capture());
        assertThat(userAllergiesCaptor.getValue()).singleElement().satisfies(added -> {
            assertThat(added.getUserId()).isEqualTo(USER_ID);
            assertThat(added.getAllergen()).isSameAs(MANGO);
        });
        assertThat(result).extracting(AllergenResponse::getCode).containsExactly("SOY", "MANGO");
    }

    @Test
    void replaceAllergies_emptyList_removesEverything() {
        UserAllergy peanutAllergy = userAllergy(11L, PEANUT);
        when(allergenRepository.findAllById(Set.of())).thenReturn(List.of());
        when(userAllergyRepository.findByUserId(USER_ID)).thenReturn(List.of(peanutAllergy));

        List<AllergenResponse> result = allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of()));

        verify(userAllergyRepository).deleteAll(List.of(peanutAllergy));
        verify(userAllergyRepository).saveAll(userAllergiesCaptor.capture());
        assertThat(userAllergiesCaptor.getValue()).isEmpty();
        assertThat(result).isEmpty();
    }

    @Test
    void replaceAllergies_unknownId_throwsAndChangesNothing() {
        when(allergenRepository.findAllById(Set.of(4L, 99L))).thenReturn(List.of(PEANUT));

        assertThatThrownBy(() -> allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of(4L, 99L))))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALLERGEN_NOT_EXISTED));
        verifyNoInteractions(userAllergyRepository);
    }

    static Allergen allergen(Long id, String code, String name, AllergenCategory category) {
        return Allergen.builder().id(id).code(code).name(name).category(category).build();
    }

    // Distinct ids matter: UserAllergy.equals ignores the allergen, so two id-less rows would be equal
    static UserAllergy userAllergy(Long id, Allergen allergen) {
        return UserAllergy.builder().id(id).userId(USER_ID).allergen(allergen).build();
    }
}
