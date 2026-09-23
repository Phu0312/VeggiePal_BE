package com.veggiepal.blog.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import com.veggiepal.blog.enums.ImageType;

public final class ImageTypeDetector {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final byte[] RIFF_SIGNATURE = "RIFF".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] WEBP_SIGNATURE = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private ImageTypeDetector() {
    }

    public static Optional<ImageType> detect(byte[] content) {

        if (content == null) {
            return Optional.empty();
        }

        if (hasSignature(content, 0, JPEG_SIGNATURE)) {
            return Optional.of(ImageType.JPEG);
        }

        if (hasSignature(content, 0, PNG_SIGNATURE)) {
            return Optional.of(ImageType.PNG);
        }

        if (hasSignature(content, 0, RIFF_SIGNATURE) && hasSignature(content, 8, WEBP_SIGNATURE)) {
            return Optional.of(ImageType.WEBP);
        }

        return Optional.empty();
    }

    private static boolean hasSignature(byte[] content, int offset, byte[] signature) {

        if (content.length < offset + signature.length) {
            return false;
        }

        return Arrays.equals(
                content, offset, offset + signature.length,
                signature, 0, signature.length
        );
    }
}
