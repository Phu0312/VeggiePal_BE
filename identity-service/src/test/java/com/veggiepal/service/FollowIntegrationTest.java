package com.veggiepal.service;

import com.veggiepal.entity.*;
import com.veggiepal.enums.*;
import com.veggiepal.repository.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:follows;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class FollowIntegrationTest {
    @Autowired FollowService service;
    @Autowired UserFollowRepository follows;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    Long one,two,three;
    @BeforeEach void setup() {
        follows.deleteAll();users.deleteAll();one=user("one");two=user("two");three=user("three");
    }
    Long user(String name) { return users.saveAndFlush(User.builder().email(name+"@example.com").fullName(name)
            .phone("0123456789").passwordHash("hash").role(Role.USER).status(UserStatus.ACTIVE).emailVerified(true).build()).getId(); }
    @Test void followAndUnfollowAreIdempotentAndSelfFollowIsForbidden() {
        service.follow(one,two);service.follow(one,two);
        assertThat(follows.count()).isEqualTo(1);
        assertThat(service.stats(one,two).followers()).isEqualTo(1);
        assertThat(service.stats(one,two).followedByMe()).isTrue();
        assertThat(service.stats(two,one).following()).isEqualTo(1);
        assertThatThrownBy(() -> service.follow(one,one)).isInstanceOf(com.veggiepal.exception.AppException.class);
        service.unfollow(one,two);service.unfollow(one,two);
        assertThat(follows.count()).isZero();
    }
    @Test void concurrentRetriesDoNotDuplicateRelations() throws Exception {
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(4)) {
            var tasks=new ArrayList<Future<?>>();
            for(int i=0;i<4;i++) tasks.add(executor.submit(() -> {start.await();return service.follow(one,two);}));
            start.countDown();for(var task:tasks) task.get(15,TimeUnit.SECONDS);
            assertThat(follows.count()).isEqualTo(1);
            assertThat(service.stats(one,two).followers()).isEqualTo(1);
        }
    }
    @Test void blockedAccountsAreHiddenWithoutDeletingRelationships() {
        service.follow(one,two);service.follow(one,three);
        User blocked=users.findById(two).orElseThrow();blocked.setStatus(UserStatus.BLOCKED);users.saveAndFlush(blocked);
        assertThat(service.list(one,one,true,0,20).items()).extracting("id").containsExactly(three);
        assertThat(service.followingIds(one)).containsExactly(three);
        assertThat(service.stats(one,one).following()).isEqualTo(1);
        assertThat(follows.count()).isEqualTo(2);
        assertThatThrownBy(() -> service.follow(two,three)).isInstanceOf(com.veggiepal.exception.AppException.class);
        assertThatThrownBy(() -> service.follow(three,two)).isInstanceOf(com.veggiepal.exception.AppException.class);
        blocked.setStatus(UserStatus.ACTIVE);users.saveAndFlush(blocked);
        assertThat(service.list(one,one,true,0,20).totalElements()).isEqualTo(2);
    }
    @Test void httpListsHavePaginationCountsAndNoPrivateContactFields() throws Exception {
        mvc.perform(put("/users/"+two+"/follow")).andExpect(status().isUnauthorized());
        mvc.perform(put("/users/"+two+"/follow").with(jwt().jwt(j -> j.claim("userId",one))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.followers").value(1));
        mvc.perform(get("/users/me/following").with(jwt().jwt(j -> j.claim("userId",one))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.items[0].id").value(two.intValue()))
                .andExpect(jsonPath("$.result.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.result.items[0].phone").doesNotExist())
                .andExpect(jsonPath("$.result.items[0].passwordHash").doesNotExist());
        mvc.perform(get("/users/me/followers").with(jwt().jwt(j -> j.claim("userId",two))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.items[0].id").value(one.intValue()));
    }
}
