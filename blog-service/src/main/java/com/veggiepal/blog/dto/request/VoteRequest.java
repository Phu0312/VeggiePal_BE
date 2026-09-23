package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteRequest {

    @NotNull(message = "INVALID_VOTE_VALUE")
    Integer value;
}
