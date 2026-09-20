package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.entity.Blog;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "moderationReason", ignore = true)
    BlogResponse toBlogResponse(Blog blog);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    BlogSummaryResponse toBlogSummaryResponse(Blog blog);
}
