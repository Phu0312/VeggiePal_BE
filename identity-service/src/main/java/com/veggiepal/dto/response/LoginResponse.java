package com.veggiepal.dto.response;

import com.veggiepal.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginResponse {
    String accessToken;
    Long userId;
    String email;
    String fullName;
    Role role;
}
