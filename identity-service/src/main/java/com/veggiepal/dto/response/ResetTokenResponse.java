package com.veggiepal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetTokenResponse {
    /** One use, for POST /auth/reset-password. */
    String resetToken;
    /** Seconds. */
    Long expiresIn;
}
