package com.veggiepal.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.recipe.entity.Recipe;
import com.veggiepal.recipe.enums.RecipeStatus;

@Repository
public interface RecipeRepository
        extends JpaRepository<Recipe, Long> {

    boolean existsByTitle(String title);

    Page<Recipe> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<Recipe> findByUserIdAndStatusNot(
            Long userId,
            RecipeStatus status,
            Pageable pageable
    );

    Page<Recipe> findByStatus(
            RecipeStatus status,
            Pageable pageable
    );

    @Query("""
            select r
            from Recipe r
            where r.status = :status
              and (
                    :keyword is null
                    or lower(r.title)
                       like lower(concat('%', :keyword, '%'))
                    or lower(r.description)
                       like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Recipe> searchPublished(
            @Param("keyword")
            String keyword,

            @Param("status")
            RecipeStatus status,

            Pageable pageable
    );

    /**
     * Atomic, like blog-service's addVoteScore: concurrent hearts must not lose counts.
     * Flush first so the pending recipe_favorites insert/delete survives the clear; clear so later reads see the count.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Recipe r set r.favoriteCount = r.favoriteCount + :delta where r.id = :id")
    void addFavoriteCount(@Param("id") Long id, @Param("delta") int delta);
}