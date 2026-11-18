package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Flujo real contra Postgres (Testcontainers): registro, login, /me, refresh y rutas protegidas. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;

    @Autowired
    AuthFlowIntegrationTest(MockMvc mockMvc, JdbcTemplate jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private MvcResult login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andReturn();
    }

    private static String refreshCookieValue(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).startsWith("refresh_token=");
        return header.substring("refresh_token=".length(), header.indexOf(';'));
    }

    private static String accessTokenOf(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return body.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void register_whenUserCreated_thenAuditColumnsAreFilled() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("audit-user", "s3cret-pass")))
                .andExpect(status().isCreated());

        var row = jdbc.queryForMap("select created_by, created_date from app_user where username = 'audit-user'");

        assertThat(row.get("created_by")).isNotNull();
        assertThat(row.get("created_date")).isNotNull();
    }

    @Test
    void authFlow_whenRegisteringAndLoggingIn_thenTokensWorkEndToEnd() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("flow-user", "s3cret-pass")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("flow-user"));

        assertThat(login("flow-user", "wrong-pass").getResponse().getStatus()).isEqualTo(401);
        assertThat(login("nobody", "whatever-pass").getResponse().getStatus()).isEqualTo(401);

        MvcResult ok = login("FLOW-USER", "s3cret-pass");
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        String access = accessTokenOf(ok);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("flow-user"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshCookieValue(ok)))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sample-items/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void register_whenSameUsernameTwice_thenConflictAndFirstPasswordKept() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("dup-user", "first-pass-1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("dup-user", "second-pass-2")))
                .andExpect(status().isConflict());

        assertThat(login("dup-user", "first-pass-1").getResponse().getStatus()).isEqualTo(200);
        assertThat(login("dup-user", "second-pass-2").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void refresh_whenAccessTokenSentAsRefreshCookie_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("swap-user", "s3cret-pass")))
                .andExpect(status().isCreated());
        String access = accessTokenOf(login("swap-user", "s3cret-pass"));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", access))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }
}
