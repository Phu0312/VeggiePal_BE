package com.veggiepal.blog.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** An MP4 (ISO base media file) opens with a box whose type, at offset 4, is "ftyp". */
public final class Mp4Detector {

    private static final byte[] FTYP = "ftyp".getBytes(StandardCharsets.US_ASCII);

    private Mp4Detector() {
    }

    // ponytail: .mov and .3gp also open with an ftyp box. Check the major brand at offset 8
    // (isom, mp41, mp42, avc1...) if those ever need to be told apart.
    public static boolean isMp4(byte[] content) {

        return content != null
                && content.length >= 12
                && Arrays.equals(content, 4, 8, FTYP, 0, FTYP.length);
    }
}
