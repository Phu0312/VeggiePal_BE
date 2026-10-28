package com.veggiepal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "RESET_TOKEN_INVALID")
    @ToString.Exclude
    String resetToken;

    // Same password rule as registration.
    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "INVALID_PASSWORD")
    @ToString.Exclude
    String newPassword;
}
