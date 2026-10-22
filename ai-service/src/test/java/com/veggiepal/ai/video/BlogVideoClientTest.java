package com.veggiepal.ai.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BlogVideoClientTest {
    @Test
    void sendsInternalTokenAndReadsMetadataEnvelope() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BlogVideoClient client = new BlogVideoClient(builder.build(), "http://blog", "secret");
        server.expect(requestTo("http://blog/internal/videos/8"))
                .andExpect(header(BlogVideoClient.INTERNAL_TOKEN_HEADER, "secret"))
                .andRespond(withSuccess("""
                        {"code":1000,"result":{"id":8,"status":"PUBLISHED","sourceType":"UPLOAD",
                        "videoUrl":"https://res.cloudinary.com/veggiepal/video/upload/v1/a.mp4"}}
                        """, MediaType.APPLICATION_JSON));

        var result = client.get(8L);

        assertEquals("UPLOAD", result.sourceType());
        server.verify();
    }

    @Test
    void mapsMissingToNotFoundAndOtherBlogFailuresToDownstreamUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BlogVideoClient client = new BlogVideoClient(builder.build(), "http://blog", "secret");
        server.expect(requestTo("http://blog/internal/videos/8")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://blog/internal/videos/9")).andRespond(withServerError());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND,
                assertThrows(AppException.class, () -> client.get(8L)).getErrorCode());

        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                assertThrows(AppException.class, () -> client.get(9L)).getErrorCode());
        server.verify();
    }

    @Test
    void unconfiguredInternalTokenFailsClosedWithoutCallingBlog() {
        BlogVideoClient client = new BlogVideoClient(RestClient.create(), "http://blog", "");
        assertEquals(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                assertThrows(AppException.class, () -> client.get(8L)).getErrorCode());
    }
}
