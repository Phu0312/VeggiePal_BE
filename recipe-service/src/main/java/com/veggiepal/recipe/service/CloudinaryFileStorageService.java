package com.veggiepal.recipe.service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.veggiepal.recipe.configuration.StorageProperties;
import com.veggiepal.recipe.exception.AppException;
import com.veggiepal.recipe.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryFileStorageService implements FileStorageService {

    private static final String IMAGE = "image";

    private static final String VIDEO = "video";

    Cloudinary cloudinary;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        // Cloudinary detects the format from the bytes and appends it to the URL itself,
        // so an extension left in the public_id would end up doubled ("a.png.png")
        Map<?, ?> options = ObjectUtils.asMap(
                "public_id", stripExtension(key),
                "resource_type", resourceTypeFor(contentType),
                "overwrite", false
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, options);
            // A null URL would be saved as the new file and the old one deleted
            return Objects.requireNonNull((String) result.get("secure_url"), "Cloudinary response has no secure_url");
        } catch (IOException | RuntimeException exception) {
            log.error("Could not upload file to Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String url) {

        if (url == null) {
            return;
        }

        // Images and videos live under different paths, and destroy needs the matching type
        String resourceType = url.startsWith(uploadUrlPrefix(VIDEO)) ? VIDEO
                : url.startsWith(uploadUrlPrefix(IMAGE)) ? IMAGE
                : null;

        if (resourceType == null) {
            return;
        }

        // v1712345678/recipes/12/abc.png -> recipes/12/abc
        String publicId = stripExtension(
                url.substring(uploadUrlPrefix(resourceType).length()).replaceFirst("^v\\d+/", ""));

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType,
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Could not delete file from Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String uploadUrlPrefix(String resourceType) {

        return "https://res.cloudinary.com/" + storageProperties.cloudName() + "/" + resourceType + "/upload/";
    }

    private static String resourceTypeFor(String contentType) {

        return contentType != null && contentType.startsWith("video/") ? VIDEO : IMAGE;
    }

    private static String stripExtension(String path) {

        int dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
    }
}
