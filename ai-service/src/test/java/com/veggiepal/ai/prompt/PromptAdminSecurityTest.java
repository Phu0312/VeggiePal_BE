package com.veggiepal.ai.prompt;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PromptAdminSecurityTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() { mvc = webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void onlyAdminCanReadPromptManagement() throws Exception {
        mvc.perform(get("/ai/admin/prompts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/ai/admin/prompts").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/ai/admin/prompts").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
