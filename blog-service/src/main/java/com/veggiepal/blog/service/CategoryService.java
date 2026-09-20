package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CategoryMapper;
import com.veggiepal.blog.repository.CategoryRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    public List<CategoryResponse> getTree(CategoryType type, boolean activeOnly) {

        List<Category> roots = type == null
                ? categoryRepository.findByParentIsNullOrderByDisplayOrderAscIdAsc()
                : categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(type);

        if (activeOnly) {
            roots = roots.stream().filter(Category::getActive).toList();
        }

        if (roots.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = roots.stream().map(Category::getId).toList();

        Map<Long, List<Category>> childrenByParent =
                categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(rootIds).stream()
                        .filter(child -> !activeOnly || child.getActive())
                        .collect(Collectors.groupingBy(child -> child.getParent().getId()));

        return roots.stream()
                .map(root -> {
                    CategoryResponse response = categoryMapper.toCategoryResponse(root);
                    response.setChildren(
                            childrenByParent.getOrDefault(root.getId(), List.of()).stream()
                                    .map(categoryMapper::toCategoryResponse)
                                    .toList()
                    );
                    return response;
                })
                .toList();
    }

    public CategoryResponse getById(Long id) {

        return categoryMapper.toCategoryResponse(findCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {

        String name = request.getName().trim();

        Category parent = null;
        CategoryType type;

        if (request.getParentId() == null) {

            if (request.getType() == null) {
                throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
            }

            type = request.getType();
            requireNameFree(categoryRepository.findByParentIsNull(), name, null);

        } else {

            parent = findCategory(request.getParentId());

            // Two levels only: a category that already has a parent cannot become one
            if (parent.getParent() != null) {
                throw new AppException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
            }

            // A child always follows its parent, so one tree cannot mix both types
            type = parent.getType();
            requireNameFree(categoryRepository.findByParentId(parent.getId()), name, null);
        }

        Category category = Category.builder()
                .parent(parent)
                .type(type)
                .name(name)
                .displayOrder(request.getDisplayOrder() == null ? (short) 0 : request.getDisplayOrder())
                .active(request.getActive() == null || request.getActive())
                .build();

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {

        Category category = findCategory(id);
        String name = request.getName().trim();

        List<Category> siblings = category.getParent() == null
                ? categoryRepository.findByParentIsNull()
                : categoryRepository.findByParentId(category.getParent().getId());

        requireNameFree(siblings, name, id);

        category.setName(name);

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {

        Category category = findCategory(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }

        categoryRepository.delete(category);
    }

    /** Used by BlogService: the category must exist and still be selectable. */
    public Category requireActiveCategory(Long id) {

        Category category = findCategory(id);

        if (!category.getActive()) {
            throw new AppException(ErrorCode.CATEGORY_INACTIVE);
        }

        return category;
    }

    private Category findCategory(Long id) {

        return categoryRepository
                .findById(id)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.CATEGORY_NOT_EXISTED
                        )
                );
    }

    private void requireNameFree(List<Category> siblings, String name, Long excludedId) {

        boolean taken = siblings.stream()
                .filter(sibling -> excludedId == null || !excludedId.equals(sibling.getId()))
                .anyMatch(sibling -> sibling.getName().equalsIgnoreCase(name));

        if (taken) {
            throw new AppException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }
}
