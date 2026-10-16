package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YouTubeUrlsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=42s",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "  http://www.youtube.com/watch?v=dQw4w9WgXcQ  "
    })
    void extractId_supportedForms_returnsTheId(String url) {
        assertThat(YouTubeUrls.extractId(url)).contains("dQw4w9WgXcQ");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com.evil.com/watch?v=dQw4w9WgXcQ",
            "javascript:alert(1)",
            "ftp://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ\"><script>",
            "https://www.youtube.com/channel/UC123",
            "https://youtu.be/",
            "https://youtu.be",
            "not a url",
            ""
    })
    void extractId_anythingElse_isEmpty(String url) {
        assertThat(YouTubeUrls.extractId(url)).isEmpty();
    }

    @Test
    void extractId_null_isEmpty() {
        assertThat(YouTubeUrls.extractId(null)).isEmpty();
    }
}
