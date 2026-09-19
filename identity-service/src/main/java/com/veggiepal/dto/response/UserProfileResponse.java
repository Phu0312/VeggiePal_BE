package com.veggiepal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    Long id;

    String email;

    String fullName;

    String phone;

    String avatarUrl;

    LocalDate dateOfBirth;

    Role role;

    UserStatus status;

    Boolean emailVerified;

    LocalDateTime createdAt;
}
