package com.veggiepal.ai.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {
    @Test void codesAreUnique() {
        assertThat(Arrays.stream(ErrorCode.values()).map(e -> e.code).toList()).doesNotHaveDuplicates();
        assertThat(ErrorCode.PROFILE_INCOMPLETE.code).isEqualTo(6020);
        assertThat(ErrorCode.AGE_NOT_SUPPORTED.code).isEqualTo(6021);
        assertThat(ErrorCode.VIDEO_QUEUE_FULL.code).isEqualTo(6025);
        assertThat(ErrorCode.VIDEO_QUEUE_FULL.status).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(ErrorCode.VIDEO_SOURCE_UNSUPPORTED.code).isEqualTo(6026);
        assertThat(ErrorCode.VIDEO_SOURCE_UNSUPPORTED.status).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
