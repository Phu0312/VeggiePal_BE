package com.veggiepal.blog.service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.veggiepal.blog.configuration.StorageProperties;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryFileStorageService implements FileStorageService {

    Cloudinary cloudinary;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        // Cloudinary detects the format from the bytes and appends it to the URL itself,
        // so an extension left in the public_id would end up doubled ("a.png.png")
        Map<?, ?> options = ObjectUtils.asMap(
                "public_id", stripExtension(key),
                "resource_type", "image",
                "overwrite", false
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, options);
            // A null URL would be saved as the new image and the old one deleted
            return Objects.requireNonNull((String) result.get("secure_url"), "Cloudinary response has no secure_url");
        } catch (IOException | RuntimeException exception) {
            log.error("Could not upload image to Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String url) {

        String prefix = uploadUrlPrefix();

        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        // v1712345678/thumbnails/12/abc.png -> thumbnails/12/abc
        String publicId = stripExtension(url.substring(prefix.length()).replaceFirst("^v\\d+/", ""));

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Could not delete image from Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String uploadUrlPrefix() {

        return "https://res.cloudinary.com/" + storageProperties.cloudName() + "/image/upload/";
    }

    private static String stripExtension(String path) {

        int dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
    }
}
