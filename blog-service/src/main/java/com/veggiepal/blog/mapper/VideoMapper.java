package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.VideoResponse;
import com.veggiepal.blog.entity.Video;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "moderationReason", ignore = true)
    VideoResponse toVideoResponse(Video video);
}
