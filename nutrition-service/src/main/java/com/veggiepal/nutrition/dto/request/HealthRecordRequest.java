package com.veggiepal.nutrition.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthRecordRequest {

    @NotNull(message = "HEIGHT_REQUIRED")
    @DecimalMin(value = "50", message = "INVALID_HEIGHT")
    @DecimalMax(value = "250", message = "INVALID_HEIGHT")
    @Digits(integer = 3, fraction = 1, message = "INVALID_HEIGHT")
    BigDecimal heightCm;

    @NotNull(message = "WEIGHT_REQUIRED")
    @DecimalMin(value = "20", message = "INVALID_WEIGHT")
    @DecimalMax(value = "300", message = "INVALID_WEIGHT")
    @Digits(integer = 3, fraction = 1, message = "INVALID_WEIGHT")
    BigDecimal weightKg;
}
