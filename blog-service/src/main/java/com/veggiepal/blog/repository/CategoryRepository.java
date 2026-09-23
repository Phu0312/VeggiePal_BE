package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByDisplayOrderAscIdAsc();

    List<Category> findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType type);

    List<Category> findByParentIdInOrderByDisplayOrderAscIdAsc(Collection<Long> parentIds);

    boolean existsByParentId(Long parentId);

    /** Names are unique across the whole tree, compared case-insensitively. */
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
