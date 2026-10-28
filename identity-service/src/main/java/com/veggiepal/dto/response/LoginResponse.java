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

    /** task_be C11: one-use, 7 days; send to POST /auth/refresh for a new pair. */
    String refreshToken;

    /** Access token lifetime in seconds. */
    Long expiresIn;
}
