package com.veggiepal.blog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteResponse {

    /** Set on a blog vote; null on a video vote. */
    Long blogId;

    /** Set on a video vote; null on a blog vote (task_be C13). */
    Long videoId;

    /** The caller's own vote: 1, -1, or null when they have not voted. */
    Integer myVote;

    Integer voteScore;
}
