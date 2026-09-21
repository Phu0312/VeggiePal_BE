package com.veggiepal.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The only user fields that appear next to public content. No email, no phone. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicUserResponse {

    Long id;

    String fullName;

    String avatarUrl;
}
