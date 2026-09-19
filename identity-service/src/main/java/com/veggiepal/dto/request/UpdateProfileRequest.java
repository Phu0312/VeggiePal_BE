package com.veggiepal.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    // null keeps the current value; when present it must contain a non-blank character
    @Pattern(regexp = "(?s).*\\S.*", message = "FULL_NAME_REQUIRED")
    String fullName;

    // "" clears the phone number
    String phone;

    @Past(message = "INVALID_DATE_OF_BIRTH")
    LocalDate dateOfBirth;
}
