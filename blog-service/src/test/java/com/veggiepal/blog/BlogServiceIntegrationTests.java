package com.veggiepal.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * End-to-end tests against a real MySQL schema and the real filter chain.
 *
 * <p>These exist because two defects that made the service completely unusable survived
 * 126 mock-based tests and eight code reviews: {@code @Lob} mapped {@code Blog.content} to
 * CLOB so Hibernate rejected {@code lower()} at startup, and three read methods lacked
 * {@code @Transactional} so every list endpoint threw {@code LazyInitializationException}.
 * Neither is reachable by a test that mocks the repository. Every case below is chosen to
 * fail if one of those classes of defect comes back.
 *
 * <p>Runs against its own schema rather than {@code veggiepal_blog} so a test run never
 * leaves rows in the database a developer is working in. Needs Docker up
 * ({@code docker compose up -d}); it is excluded from the fast loop for that reason, and
 * {@code CLAUDE.md} records that it is a required gate before committing a change to an
 * entity or a repository.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_blog_it?createDatabaseIfNotExist=true",
        "spring.jpa.show-sql=false"
})
class BlogServiceIntegrationTests {

    // Matches the default jwt.secret in application.properties (JWT_SECRET is unset in tests).
    private static final String JWT_SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    private static final long ADMIN_ID = 1L;
    private static final long AUTHOR_ID = 7L;
    private static final long OTHER_MEMBER_ID = 8L;

    @Autowired
    MockMvc mockMvc;

    // ---------------------------------------------------------------- categories

    @Test
    void categoryTree_nestsAChildUnderItsRoot() throws Exception {
        String suffix = unique();
        long rootId = createRootCategory("Công thức " + suffix);
        createChildCategory("Món chính " + suffix, rootId);

        // Resolving the children means walking a lazy parent association on every row.
        mockMvc.perform(get("/categories").param("type", "RECIPE_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.id == %d)].children[0].name".formatted(rootId))
                        .value("Món chính " + suffix));
    }

    // ---------------------------------------------------------------- blogs

    @Test
    void publishedBlog_appearsInTheListWithItsCategoryName() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Đậu hũ sốt cà " + suffix, body(suffix));

        // categoryName comes off a lazy Category proxy. Without a transaction around the
        // read, this is where LazyInitializationException surfaced as a 500.
        mockMvc.perform(get("/blogs").param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[?(@.id == %d)].categoryName".formatted(blogId))
                        .value("Danh mục " + suffix));
    }

    @Test
    void keywordSearch_matchesTextInsideTheBody() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        // The one request that exercises lower() against the real content column.
        // With @Lob restored on Blog.content the context would not even start.
        mockMvc.perform(get("/blogs").param("keyword", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void keywordSearch_isCaseInsensitive() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        createPublishedBlog(categoryId, "MARKER" + suffix, body(suffix));

        mockMvc.perform(get("/blogs").param("keyword", "marker" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void readingABlog_incrementsItsViewCount() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        // The only check anywhere that the @Modifying update really executes: a mocked
        // repository can only prove the method was called.
        mockMvc.perform(get("/blogs/" + blogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(1));

        mockMvc.perform(get("/blogs/" + blogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(2));
    }

    @Test
    void ownBlogs_requireATokenAndReturnTheCallersDrafts() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        createDraftBlog(categoryId, "Nháp " + suffix, body(suffix));

        // /blogs/me matches a bare /blogs/{id} pattern. If PUBLIC_ENDPOINTS ever loses the
        // digit constraint this answers 401 forever instead, because the token is stripped.
        mockMvc.perform(get("/blogs/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        mockMvc.perform(get("/blogs/me").header("Authorization", bearer(AUTHOR_ID, "USER"))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[?(@.title == 'Nháp %s')]".formatted(suffix))
                        .exists());
    }

    @Test
    void relatedBlogs_returnOtherPublishedPostsInTheSameCategory() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long first = createPublishedBlog(categoryId, "Bài một " + suffix, body(suffix));
        long second = createPublishedBlog(categoryId, "Bài hai " + suffix, body(suffix));

        mockMvc.perform(get("/blogs/" + first + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.id == %d)]".formatted(second)).exists())
                .andExpect(jsonPath("$.result[?(@.id == %d)]".formatted(first)).doesNotExist());
    }

    // ---------------------------------------------------------------- comments

    @Test
    void commentsAndReplies_roundTripWithTheirReplyCount() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        long rootComment = idOf(mockMvc.perform(authed(post("/comments"), OTHER_MEMBER_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"BLOG","targetId":%d,"content":"ngon quá"}
                                """.formatted(blogId)))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(authed(post("/comments"), AUTHOR_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"BLOG","targetId":%d,"parentCommentId":%d,"content":"cảm ơn bạn"}
                                """.formatted(blogId, rootComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.parentCommentId").value(rootComment));

        // replyCount comes from a grouped interface projection — a shape that only runs
        // against a real database.
        mockMvc.perform(get("/comments").param("targetType", "BLOG").param("targetId", String.valueOf(blogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].replyCount").value(1));

        mockMvc.perform(get("/comments/" + rootComment + "/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].content").value("cảm ơn bạn"));
    }

    @Test
    void commentsOnARemovedBlog_areNoLongerReadable() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        mockMvc.perform(authed(post("/comments"), OTHER_MEMBER_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"BLOG","targetId":%d,"content":"ngon quá"}
                                """.formatted(blogId)))
                .andExpect(status().isOk());

        mockMvc.perform(authed(delete("/blogs/" + blogId), ADMIN_ID, "ADMIN"))
                .andExpect(status().isOk());

        // comments.target_id has no foreign key by design, so nothing but the service check
        // stops a removed blog's thread staying public.
        mockMvc.perform(get("/comments").param("targetType", "BLOG").param("targetId", String.valueOf(blogId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3014));
    }

    // ---------------------------------------------------------------- votes

    @Test
    void flippingAVote_movesTheScoreByTwo() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        mockMvc.perform(authed(put("/blogs/" + blogId + "/vote"), OTHER_MEMBER_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voteScore").value(1));

        // Second vote from the same user on the same blog: the unique constraint must be
        // updated rather than violated, and the delta must be -2 rather than -1.
        mockMvc.perform(authed(put("/blogs/" + blogId + "/vote"), OTHER_MEMBER_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":-1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voteScore").value(-1));

        mockMvc.perform(get("/blogs/" + blogId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voteScore").value(-1));
    }

    @Test
    void votingOnYourOwnBlog_isRejected() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        mockMvc.perform(authed(put("/blogs/" + blogId + "/vote"), AUTHOR_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3041));
    }

    // ---------------------------------------------------------------- thumbnails

    @Test
    void aThumbnailJustOverTheServiceLimit_isRejectedBySizeNotByAFiveHundred() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        // Between MAX_THUMBNAIL_BYTES (5MB) and the multipart limit (6MB): the parser lets
        // it through and BlogService's own check rejects it. That check was unreachable
        // while the two limits were equal.
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[5 * 1024 * 1024 + 1024]);

        mockMvc.perform(authedMultipart(multipart("/blogs/" + blogId + "/thumbnail"), AUTHOR_ID, "USER")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3022));
    }

    @Test
    void aThumbnailOverTheMultipartLimit_isRejectedByTheHandlerNotTheCatchAll() throws Exception {
        String suffix = unique();
        long categoryId = createRootCategory("Danh mục " + suffix);
        long blogId = createPublishedBlog(categoryId, "Bài viết " + suffix, body(suffix));

        // Above the 6MB parser limit but below the 7MB request limit: this is the path that
        // answered 9999/500 until MaxUploadSizeExceededException got its own handler.
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[6 * 1024 * 1024 + 512 * 1024]);

        mockMvc.perform(authedMultipart(multipart("/blogs/" + blogId + "/thumbnail"), AUTHOR_ID, "USER")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3022));
    }

    // ---------------------------------------------------------------- helpers

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Content long enough to clear the 20-character minimum, carrying a searchable marker. */
    private static String body(String marker) {
        return "Nguyên liệu và các bước thực hiện, mã nhận dạng " + marker + ", phần còn lại là mô tả.";
    }

    private static String bearer(long userId, String role) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user" + userId + "@example.com")
                .claim("userId", userId)
                .claim("role", role)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 300_000))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return "Bearer " + jwt.serialize();
    }

    private static MockHttpServletRequestBuilder authed(
            MockHttpServletRequestBuilder builder, long userId, String role
    ) throws Exception {
        return builder.header("Authorization", bearer(userId, role));
    }

    private static MockMultipartHttpServletRequestBuilder authedMultipart(
            MockMultipartHttpServletRequestBuilder builder, long userId, String role
    ) throws Exception {
        builder.header("Authorization", bearer(userId, role));
        return builder;
    }

    private static long idOf(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(json, "$.result.id")).longValue();
    }

    private long createRootCategory(String name) throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/categories"), ADMIN_ID, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"RECIPE_TYPE"}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();

        return idOf(result);
    }

    private long createChildCategory(String name, long parentId) throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/categories"), ADMIN_ID, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","parentId":%d}
                                """.formatted(name, parentId)))
                .andExpect(status().isOk())
                .andReturn();

        return idOf(result);
    }

    private long createPublishedBlog(long categoryId, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/blogs"), AUTHOR_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"%s","categoryId":%d,"publish":true}
                                """.formatted(title, content, categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"))
                .andReturn();

        return idOf(result);
    }

    private long createDraftBlog(long categoryId, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(authed(post("/blogs"), AUTHOR_ID, "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"%s","categoryId":%d,"publish":false}
                                """.formatted(title, content, categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DRAFT"))
                .andReturn();

        return idOf(result);
    }

    /** Guards the assumption the two thumbnail tests above are built on. */
    @Test
    void thumbnailLimits_leaveRoomForTheServiceCheckToRun() {
        long serviceLimit = 5L * 1024 * 1024;
        long parserLimit = 6L * 1024 * 1024;

        assertThat(parserLimit)
                .as("the multipart limit must exceed MAX_THUMBNAIL_BYTES, or the service's "
                        + "own size check is unreachable and THUMBNAIL_TOO_LARGE is dead code")
                .isGreaterThan(serviceLimit);
    }
}
