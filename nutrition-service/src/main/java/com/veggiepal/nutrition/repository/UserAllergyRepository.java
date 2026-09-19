package com.veggiepal.nutrition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    List<UserAllergy> findByUserId(Long userId);

    @Query("select ua.allergen from UserAllergy ua where ua.userId = :userId")
    List<Allergen> findAllergensByUserId(@Param("userId") Long userId);
}
