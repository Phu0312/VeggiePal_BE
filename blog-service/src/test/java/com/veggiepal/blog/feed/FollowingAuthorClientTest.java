package com.veggiepal.blog.feed;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import com.veggiepal.blog.exception.*;
class FollowingAuthorClientTest {
    HttpServer server;String url;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        url="http://127.0.0.1:"+server.getAddress().getPort();server.start();
    }
    @AfterEach void cleanup() { server.stop(0); }
    @Test void forwardsTokenAndReturnsOnlyIdentityProvidedIds() {
        var auth=new AtomicReference<String>();
        server.createContext("/users/me/following/ids",e -> {
            auth.set(e.getRequestHeaders().getFirst("Authorization"));
            byte[] body="{\"code\":1000,\"result\":[7,8,7]}".getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,body.length);
            try(var out=e.getResponseBody()) { out.write(body); }
        });
        assertThat(new FollowingAuthorClient(url).activeFollowingIds("owner-token")).containsExactly(7L,8L);
        assertThat(auth.get()).isEqualTo("Bearer owner-token");
    }
    @Test void identityFailureDoesNotFallBackToPublicFeed() {
        server.createContext("/users/me/following/ids",e -> {e.sendResponseHeaders(503,-1);e.close();});
        assertThatThrownBy(() -> new FollowingAuthorClient(url).activeFollowingIds("token"))
                .isInstanceOf(AppException.class).extracting("errorCode").isEqualTo(ErrorCode.COMMUNITY_UNAVAILABLE);
    }
}
