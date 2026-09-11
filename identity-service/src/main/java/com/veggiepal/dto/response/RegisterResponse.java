package com.veggiepal.dto.response;

import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterResponse {
    Long id;

    String email;

    String fullName;

    String phone;

    String avatarUrl;

    Role role;

    UserStatus status;

    Boolean emailVerified;
}
