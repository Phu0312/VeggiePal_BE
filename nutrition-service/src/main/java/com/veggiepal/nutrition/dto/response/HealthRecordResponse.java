package com.veggiepal.nutrition.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthRecordResponse {
    Long id;

    BigDecimal heightCm;

    BigDecimal weightKg;

    BigDecimal bmi;

    LocalDateTime recordedAt;
}
