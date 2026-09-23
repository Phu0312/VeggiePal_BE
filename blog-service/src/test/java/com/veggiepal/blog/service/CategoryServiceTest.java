package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CategoryMapper;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BlogRepository blogRepository;

    @Spy
    CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @InjectMocks
    CategoryService categoryService;

    static Category root(Long id, String name) {
        return Category.builder()
                .id(id).name(name).type(CategoryType.RECIPE_TYPE)
                .displayOrder((short) 0).active(true)
                .build();
    }

    static Category child(Long id, String name, Category parent) {
        return Category.builder()
                .id(id).name(name).parent(parent).type(parent.getType())
                .displayOrder((short) 0).active(true)
                .build();
    }

    @Test
    void create_root_savesWithGivenTypeAndDefaults() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.create(CategoryRequest.builder()
                .name("  Món chính  ")
                .type(CategoryType.RECIPE_TYPE)
                .build());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());

        assertThat(saved.getValue().getName()).isEqualTo("Món chính");
        assertThat(saved.getValue().getParent()).isNull();
        assertThat(saved.getValue().getType()).isEqualTo(CategoryType.RECIPE_TYPE);
        assertThat(saved.getValue().getDisplayOrder()).isZero();
        assertThat(saved.getValue().getActive()).isTrue();
    }

    @Test
    void create_rootWithoutType_throwsTypeRequired() {
        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("Món chính").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_TYPE_REQUIRED);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_child_inheritsParentTypeEvenWhenRequestSaysOtherwise() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.create(CategoryRequest.builder()
                .name("Món phụ")
                .parentId(1L)
                .type(CategoryType.FOOD_TYPE)
                .build());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());

        assertThat(saved.getValue().getType()).isEqualTo(CategoryType.RECIPE_TYPE);
    }

    @Test
    void create_grandChild_throwsDepthExceeded() {
        Category parent = root(1L, "Công thức");
        Category level2 = child(2L, "Món chính", parent);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(level2));

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("Món xào").parentId(2L).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
    }

    // Task sheet US5: "Tên danh mục (name) phải là Unique" — across the whole tree, not per
    // parent. The per-parent rule this replaces let the same name sit under two roots.
    @Test
    void create_nameAlreadyUsedUnderAnotherParent_throwsDuplicated() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.existsByNameIgnoreCase("Món chính")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("  Món chính ").parentId(1L).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATED);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_rootNameAlreadyUsed_throwsDuplicated() {
        when(categoryRepository.existsByNameIgnoreCase("Công thức")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("Công thức").type(CategoryType.RECIPE_TYPE).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    // The category's own name does exist — it is this row. Only a check that excludes the
    // row being updated lets it keep its name; a plain existence check would reject it.
    @Test
    void update_keepingItsOwnName_isAllowed() {
        Category parent = root(1L, "Công thức");
        Category target = child(2L, "Món chính", parent);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(target));
        lenient().when(categoryRepository.existsByNameIgnoreCase("Món chính")).thenReturn(true);
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Món chính", 2L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.update(2L, CategoryRequest.builder()
                .name("Món chính").displayOrder((short) 5).active(false).build());

        assertThat(target.getDisplayOrder()).isEqualTo((short) 5);
        assertThat(target.getActive()).isFalse();
    }

    @Test
    void update_toANameAnotherCategoryUses_throwsDuplicated() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(child(2L, "Món chính", parent)));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Món phụ", 2L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(2L,
                CategoryRequest.builder().name("Món phụ").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATED);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_withChildren_throwsInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root(1L, "Công thức")));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_leafCategory_deletes() {
        Category leaf = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(leaf));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(blogRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(leaf);
    }

    @Test
    void delete_stillUsedByBlogs_throwsInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root(1L, "Công thức")));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(blogRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void requireActiveCategory_inactive_throwsCategoryInactive() {
        Category inactive = root(1L, "Công thức");
        inactive.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> categoryService.requireActiveCategory(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);
    }

    @Test
    void requireActiveCategory_missing_throwsNotExisted() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.requireActiveCategory(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_EXISTED);
    }

    // Task 3 review finding: an active child under a deactivated root must not
    // stay individually selectable just because getTree() hides it from the tree.
    @Test
    void requireActiveCategory_activeChildOfInactiveParent_throwsCategoryInactive() {
        Category parent = root(1L, "Công thức");
        parent.setActive(false);
        Category activeChild = child(2L, "Món chính", parent);
        activeChild.setActive(true);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(activeChild));

        assertThatThrownBy(() -> categoryService.requireActiveCategory(2L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);
    }

    @Test
    void getTree_nestsChildrenUnderRoots() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType.RECIPE_TYPE))
                .thenReturn(List.of(parent));
        when(categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(List.of(1L)))
                .thenReturn(List.of(child(2L, "Món chính", parent), child(3L, "Món phụ", parent)));

        List<CategoryResponse> tree = categoryService.getTree(CategoryType.RECIPE_TYPE, true);

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getChildren())
                .extracting(CategoryResponse::getName)
                .containsExactly("Món chính", "Món phụ");
    }

    @Test
    void getTree_activeOnly_dropsInactiveChildren() {
        Category parent = root(1L, "Công thức");
        Category hidden = child(3L, "Món phụ", parent);
        hidden.setActive(false);

        when(categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType.RECIPE_TYPE))
                .thenReturn(List.of(parent));
        when(categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(List.of(1L)))
                .thenReturn(List.of(child(2L, "Món chính", parent), hidden));

        List<CategoryResponse> tree = categoryService.getTree(CategoryType.RECIPE_TYPE, true);

        assertThat(tree.getFirst().getChildren())
                .extracting(CategoryResponse::getName)
                .containsExactly("Món chính");
    }
}
