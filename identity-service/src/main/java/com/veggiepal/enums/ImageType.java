package com.veggiepal.enums;

import lombok.Getter;

@Getter
public enum ImageType {

    JPEG("image/jpeg", "jpg"),

    PNG("image/png", "png"),

    WEBP("image/webp", "webp");

    ImageType(
            String contentType,
            String extension
    ) {
        this.contentType = contentType;
        this.extension = extension;
    }

    final String contentType;

    final String extension;
}
