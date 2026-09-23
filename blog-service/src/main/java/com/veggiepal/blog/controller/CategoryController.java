package com.veggiepal.blog.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category", description = "Content categories by food type and recipe type")
public class CategoryController {

    CategoryService categoryService;

    @Operation(summary = "Category tree, two levels deep")
    @GetMapping
    ApiResponse<List<CategoryResponse>> getTree(
            @RequestParam(name = "type", required = false) CategoryType type,
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {

        return ApiResponse
                .<List<CategoryResponse>>builder()
                .result(categoryService.getTree(type, activeOnly))
                .build();
    }

    @Operation(summary = "One category")
    @GetMapping("/{id}")
    ApiResponse<CategoryResponse> getById(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.getById(id))
                .build();
    }

    @Operation(summary = "Create a category (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    ApiResponse<CategoryResponse> create(
            @RequestBody @Valid CategoryRequest request
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.create(request))
                .build();
    }

    @Operation(summary = "Rename, reorder or hide a category (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    ApiResponse<CategoryResponse> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid CategoryRequest request
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.update(id, request))
                .build();
    }

    @Operation(summary = "Delete a category that nothing uses (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @PathVariable("id") Long id
    ) {

        categoryService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
