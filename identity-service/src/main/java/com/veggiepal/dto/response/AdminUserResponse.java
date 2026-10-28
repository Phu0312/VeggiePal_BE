package com.veggiepal.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** task_be C12. Never carries the password hash. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserResponse {

    Long id;

    String email;

    String fullName;

    String avatarUrl;

    Role role;

    UserStatus status;

    Boolean emailVerified;

    LocalDateTime createdAt;

    String lockedReason;

    LocalDateTime lockedAt;
}
