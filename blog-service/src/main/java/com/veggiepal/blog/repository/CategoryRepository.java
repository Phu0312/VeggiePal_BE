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

    /** Unordered sibling lookup, used for the duplicate-name check. */
    List<Category> findByParentIsNull();

    List<Category> findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType type);

    List<Category> findByParentIdInOrderByDisplayOrderAscIdAsc(Collection<Long> parentIds);

    List<Category> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);
}
