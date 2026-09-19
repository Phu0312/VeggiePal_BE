package com.veggiepal.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.Allergen;

@Repository
public interface AllergenRepository extends JpaRepository<Allergen, Long> {
}
