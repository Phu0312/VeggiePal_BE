package com.veggiepal.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.veggiepal.dto.response.ApiResponse;

class GlobalExceptionHandlerUnitTest {

    @Test
    void handlingMaxUploadSize_returnsAvatarTooLarge() {
        ResponseEntity<ApiResponse<?>> response = new GlobalExceptionHandler()
                .handlingMaxUploadSize(new MaxUploadSizeExceededException(2L * 1024 * 1024));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(1016);
        assertThat(response.getBody().getMessage()).isEqualTo("Avatar must not exceed 2MB");
    }
}
