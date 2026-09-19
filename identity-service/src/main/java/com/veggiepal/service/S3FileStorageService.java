package com.veggiepal.service;

import org.springframework.stereotype.Service;

import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class S3FileStorageService implements FileStorageService {

    S3Client s3Client;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            log.error("Could not upload object to storage", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return publicUrlPrefix() + key;
    }

    @Override
    public void delete(String url) {

        String prefix = publicUrlPrefix();

        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(url.substring(prefix.length()))
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            log.error("Could not delete object from storage", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String publicUrlPrefix() {

        return storageProperties.publicUrl() + "/";
    }
}
