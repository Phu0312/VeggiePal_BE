package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloudinaryVideoDownloaderTest {
    private VideoStorage storage;
    private HttpClient http;
    private VideoProperties properties;

    @BeforeEach
    void setUp() {
        storage = mock(VideoStorage.class);
        http = mock(HttpClient.class);
        properties = new VideoProperties("ffmpeg", "ffprobe", 100, 1800, Duration.ofMinutes(2),
                Duration.ofSeconds(20), 1, 1, "build/video", Duration.ofMinutes(20),
                Duration.ofDays(30), Duration.ofMinutes(5), 4000);
    }

    @Test
    void rejectsNonCloudinaryWrongAccountHttpAndPrivateDnsAsSsrf() throws Exception {
        var publicDns = downloader(host -> new InetAddress[]{InetAddress.getByName("8.8.8.8")});
        for (String url : List.of(
                "http://res.cloudinary.com/veggiepal/video/upload/a.mp4",
                "https://evil.example/veggiepal/video/upload/a.mp4",
                "https://res.cloudinary.com/other/video/upload/a.mp4",
                "https://res.cloudinary.com/veggiepal/image/upload/a.mp4")) {
            assertEquals(ErrorCode.VIDEO_PROCESSING_FAILED,
                    assertThrows(AppException.class, () -> publicDns.download(url)).getErrorCode());
        }

        var privateDns = downloader(host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")});
        assertEquals(ErrorCode.VIDEO_PROCESSING_FAILED, assertThrows(AppException.class,
                () -> privateDns.download("https://res.cloudinary.com/veggiepal/video/upload/a.mp4"))
                .getErrorCode());
        verify(http, never()).send(any(), any());
    }

    @Test
    void rejectsRedirectsAndOversizedContentLengthWithoutWriting() throws Exception {
        @SuppressWarnings("unchecked") HttpResponse<java.io.InputStream> response = mock(HttpResponse.class);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(response.body()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(response.statusCode()).thenReturn(302);
        var downloader = downloader(host -> new InetAddress[]{InetAddress.getByName("8.8.8.8")});

        assertEquals(ErrorCode.VIDEO_PROCESSING_FAILED, assertThrows(AppException.class,
                () -> downloader.download("https://res.cloudinary.com/veggiepal/video/upload/a.mp4"))
                .getErrorCode());

        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Length", List.of("101")), (name, value) -> true));
        assertEquals(ErrorCode.FILE_TOO_LARGE, assertThrows(AppException.class,
                () -> downloader.download("https://res.cloudinary.com/veggiepal/video/upload/a.mp4"))
                .getErrorCode());
        verify(storage, never()).store(any(java.io.InputStream.class), anyLong());
    }

    private CloudinaryVideoDownloader downloader(CloudinaryVideoDownloader.HostResolver resolver) {
        return new CloudinaryVideoDownloader(storage, properties, "veggiepal", Duration.ofSeconds(5), http,
                resolver);
    }
}
