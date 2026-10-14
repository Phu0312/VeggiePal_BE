package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CloudinaryFileStorageServiceTest {

    private static final StorageProperties PROPERTIES =
            new StorageProperties("demo-cloud", "api-key", "api-secret");

    private static final String IMAGE_URL =
            "https://res.cloudinary.com/demo-cloud/image/upload/v1712345678/avatars/7/a.png";

    @Mock
    Cloudinary cloudinary;

    @Mock
    Uploader uploader;

    CloudinaryFileStorageService storageService;

    @BeforeEach
    void setUp() {
        // lenient: the null/foreign URL case never reaches the SDK
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        storageService = new CloudinaryFileStorageService(cloudinary, PROPERTIES);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void upload_sendsPublicIdWithoutExtensionAndReturnsSecureUrl() throws IOException {
        byte[] content = {1, 2, 3};
        when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", IMAGE_URL));

        String url = storageService.upload("avatars/7/a.png", content, "image/png");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(content), options.capture());
        assertThat((Map<String, Object>) options.getValue())
                .containsEntry("public_id", "avatars/7/a")
                .containsEntry("resource_type", "image")
                .containsEntry("overwrite", false);
        assertThat(url).isEqualTo(IMAGE_URL);
    }

    @Test
    void upload_networkFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void upload_apiError_throwsFileUploadFailed() throws IOException {
        // The SDK reports API errors (bad credentials, missing cloud_name) as RuntimeException
        when(uploader.upload(any(), anyMap())).thenThrow(new RuntimeException("Invalid Signature"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void upload_responseWithoutSecureUrl_throwsFileUploadFailed() throws IOException {
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of());

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void delete_versionedUrl_destroysByPublicId() throws IOException {
        storageService.delete(IMAGE_URL);

        verify(uploader).destroy(eq("avatars/7/a"), anyMap());
    }

    @Test
    void delete_unversionedUrl_destroysByPublicId() throws IOException {
        storageService.delete("https://res.cloudinary.com/demo-cloud/image/upload/avatars/7/a.png");

        verify(uploader).destroy(eq("avatars/7/a"), anyMap());
    }

    @Test
    void delete_nullOrForeignUrl_doesNothing() {
        storageService.delete(null);
        storageService.delete("http://localhost:9000/veggiepal-avatars/avatars/7/a.png");
        storageService.delete("https://res.cloudinary.com/other-cloud/image/upload/v1/avatars/7/a.png");

        verifyNoInteractions(uploader);
    }

    @Test
    void delete_storageFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.delete(IMAGE_URL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }
}
