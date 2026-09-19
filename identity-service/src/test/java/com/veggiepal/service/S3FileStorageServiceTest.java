package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    private static final StorageProperties PROPERTIES = new StorageProperties(
            "http://localhost:9000",
            "us-east-1",
            "minioadmin",
            "minioadmin",
            "veggiepal-avatars",
            "http://localhost:9000/veggiepal-avatars"
    );

    private static final String OBJECT_URL =
            "http://localhost:9000/veggiepal-avatars/avatars/7/a.png";

    @Mock
    S3Client s3Client;

    S3FileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3FileStorageService(s3Client, PROPERTIES);
    }

    @Test
    void upload_putsObjectAndReturnsPublicUrl() {
        String url = storageService.upload("avatars/7/a.png", new byte[]{1, 2, 3}, "image/png");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("veggiepal-avatars");
        assertThat(captor.getValue().key()).isEqualTo("avatars/7/a.png");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        assertThat(url).isEqualTo(OBJECT_URL);
    }

    @Test
    void upload_storageFailure_throwsFileUploadFailed() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("storage down"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void delete_ownUrl_deletesObjectByKey() {
        storageService.delete(OBJECT_URL);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("veggiepal-avatars");
        assertThat(captor.getValue().key()).isEqualTo("avatars/7/a.png");
    }

    @Test
    void delete_nullOrForeignUrl_doesNothing() {
        storageService.delete(null);
        storageService.delete("https://cdn.example.com/avatars/7/a.png");

        verifyNoInteractions(s3Client);
    }

    @Test
    void delete_storageFailure_throwsFileUploadFailed() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("storage down"));

        assertThatThrownBy(() -> storageService.delete(OBJECT_URL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }
}
