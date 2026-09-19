package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.veggiepal.enums.ImageType;

class ImageTypeDetectorTest {

    static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

    static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    static final byte[] WEBP_BYTES = {'R', 'I', 'F', 'F', 0x10, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @Test
    void detect_recognisesSupportedImages() {
        assertThat(ImageTypeDetector.detect(JPEG_BYTES)).contains(ImageType.JPEG);
        assertThat(ImageTypeDetector.detect(PNG_BYTES)).contains(ImageType.PNG);
        assertThat(ImageTypeDetector.detect(WEBP_BYTES)).contains(ImageType.WEBP);
    }

    @Test
    void detect_rejectsOtherContent() {
        assertThat(ImageTypeDetector.detect("GIF89a-not-supported".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(ImageTypeDetector.detect("<html></html>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    @Test
    void detect_rejectsTooShortOrNull() {
        assertThat(ImageTypeDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(ImageTypeDetector.detect(new byte[]{'R', 'I', 'F', 'F', 0x10})).isEmpty();
        assertThat(ImageTypeDetector.detect(null)).isEmpty();
    }
}
