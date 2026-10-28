package com.veggiepal.recipe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.entity.RecipeFavorite;
import com.veggiepal.recipe.enums.RecipeStatus;

@Repository
public interface RecipeFavoriteRepository extends JpaRepository<RecipeFavorite, Long> {

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    Optional<RecipeFavorite> findByUserIdAndRecipeId(Long userId, Long recipeId);

    // Most recently loved first; hidden or archived recipes drop out of the collection
    @Query(value = """
            select f.recipe from RecipeFavorite f
            where f.userId = :userId and f.recipe.status = :status
            order by f.createdAt desc, f.id desc
            """,
            countQuery = """
            select count(f) from RecipeFavorite f
            where f.userId = :userId and f.recipe.status = :status
            """)
    Page<Recipe> findFavoriteRecipes(
            @Param("userId") Long userId,
            @Param("status") RecipeStatus status,
            Pageable pageable
    );

    @Query("select f.recipe.id from RecipeFavorite f where f.userId = :userId")
    List<Long> findRecipeIdsByUserId(@Param("userId") Long userId);
}
