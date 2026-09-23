package com.veggiepal.blog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteResponse {

    Long blogId;

    /** The caller's own vote: 1, -1, or null when they have not voted. */
    Integer myVote;

    Integer voteScore;
}
