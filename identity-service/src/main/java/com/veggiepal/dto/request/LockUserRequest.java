package com.veggiepal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class LockUserRequest {

    // Task sheet US16: the admin must type why
    @NotBlank(message = "LOCK_REASON_REQUIRED")
    @Size(max = 500, message = "LOCK_REASON_REQUIRED")
    String reason;
}
