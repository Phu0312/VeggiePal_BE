package com.veggiepal.ai.video;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryVideoDownloader {
    static final String CLOUDINARY_HOST = "res.cloudinary.com";

    private final VideoStorage storage;
    private final VideoProperties videoProperties;
    private final String cloudName;
    private final Duration readTimeout;
    private final HttpClient http;
    private final HostResolver resolver;

    @Autowired
    public CloudinaryVideoDownloader(VideoStorage storage, VideoProperties videoProperties,
            @Value("${storage.cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}") String cloudName,
            @Value("${ai.video.download-connect-timeout:5s}") Duration connectTimeout,
            @Value("${ai.video.download-read-timeout:30s}") Duration readTimeout) {
        this(storage, videoProperties, cloudName, readTimeout,
                HttpClient.newBuilder().connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER).build(),
                host -> InetAddress.getAllByName(host));
    }

    CloudinaryVideoDownloader(VideoStorage storage, VideoProperties videoProperties, String cloudName,
            Duration readTimeout, HttpClient http, HostResolver resolver) {
        this.storage = storage;
        this.videoProperties = videoProperties;
        this.cloudName = cloudName;
        this.readTimeout = readTimeout;
        this.http = http;
        this.resolver = resolver;
    }

    public Path download(String rawUrl) {
        try {
            URI uri = validateUri(rawUrl);
            validatePublicDns(uri.getHost());
            HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(readTimeout).build();
            HttpResponse<java.io.InputStream> response = http.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED);
            }
            long length = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (length > videoProperties.maxFileBytes()) {
                response.body().close();
                throw new AppException(ErrorCode.FILE_TOO_LARGE);
            }
            return storage.store(response.body(), videoProperties.maxFileBytes());
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        }
    }

    URI validateUri(String rawUrl) {
        if (cloudName == null || cloudName.isBlank() || rawUrl == null) fail();
        try {
            URI uri = URI.create(rawUrl);
            String expectedPrefix = "/" + cloudName + "/video/upload/";
            String path = uri.getRawPath();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || !CLOUDINARY_HOST.equals(uri.getHost().toLowerCase(Locale.ROOT))
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || path == null || !path.startsWith(expectedPrefix)
                    || path.length() == expectedPrefix.length()
                    || !uri.normalize().equals(uri)) {
                fail();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        }
    }

    private void validatePublicDns(String host) {
        try {
            InetAddress[] addresses = resolver.resolve(host);
            if (addresses.length == 0) fail();
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                        || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isMulticastAddress() || uniqueLocal(address)) {
                    fail();
                }
            }
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED, exception);
        }
    }

    private static boolean uniqueLocal(InetAddress address) {
        return address instanceof Inet6Address && (address.getAddress()[0] & 0xfe) == 0xfc;
    }

    private static void fail() { throw new AppException(ErrorCode.VIDEO_PROCESSING_FAILED); }

    @FunctionalInterface
    interface HostResolver { InetAddress[] resolve(String host) throws Exception; }
}
