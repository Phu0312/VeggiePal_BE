package com.veggiepal.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.AdminUserResponse;
import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "activityLevel", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lockedReason", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "lockedBy", ignore = true)
    User toUser(RegisterRequest request);

    AdminUserResponse toAdminUserResponse(User user);

    RegisterResponse toUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    PublicUserResponse toPublicUserResponse(User user);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    @Mapping(target = "gender", source = "gender")
    @Mapping(target = "activityLevel", source = "activityLevel")
    void updateProfile(@MappingTarget User user, UpdateProfileRequest request);
}
