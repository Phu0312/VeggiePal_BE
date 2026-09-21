package com.veggiepal.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Lets other services' clients resolve an author id into a display name.
 * blog-service stores only author_id and never calls here itself.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicUserService {

    static final int MAX_IDS = 50;

    UserRepository userRepository;
    UserMapper userMapper;

    public List<PublicUserResponse> getPublicUsers(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // This endpoint is public: a cap keeps it from being a users-table dump
        if (ids.size() > MAX_IDS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Suspended and pending accounts stay invisible
        return userRepository
                .findByIdInAndStatus(ids, UserStatus.ACTIVE)
                .stream()
                .map(userMapper::toPublicUserResponse)
                .toList();
    }
}
