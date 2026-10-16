package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class Mp4DetectorTest {

    static byte[] mp4Bytes() {
        byte[] content = new byte[32];
        // box size 0x20, then the box type "ftyp", then the major brand "isom"
        content[3] = 0x20;
        System.arraycopy("ftypisom".getBytes(StandardCharsets.US_ASCII), 0, content, 4, 8);
        return content;
    }

    @Test
    void isMp4_ftypBoxAtOffsetFour_isTrue() {
        assertThat(Mp4Detector.isMp4(mp4Bytes())).isTrue();
    }

    @Test
    void isMp4_pngBytes_isFalse() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        assertThat(Mp4Detector.isMp4(png)).isFalse();
    }

    @Test
    void isMp4_tooShortOrNull_isFalse() {
        assertThat(Mp4Detector.isMp4(new byte[]{0, 0, 0, 0x20, 'f', 't', 'y', 'p'})).isFalse();
        assertThat(Mp4Detector.isMp4(null)).isFalse();
    }
}
