package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "parentCommentId", source = "parent.id")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    CommentResponse toCommentResponse(Comment comment);
}
