package com.veggiepal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @Email(message = "INVALID_EMAIL")
    @NotBlank(message = "EMAIL_REQUIRED")
    String email;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName;

    String phone;
}
