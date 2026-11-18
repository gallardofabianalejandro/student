package dev.fabian.gallardo.student.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fabian.gallardo.student.api.rest.AuthController;
import dev.fabian.gallardo.student.api.rest.SampleItemController;
import dev.fabian.gallardo.student.domain.exception.InvalidCredentialsException;
import dev.fabian.gallardo.student.domain.model.AuthTokens;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.port.in.CreateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.DeleteSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.ListSampleItemsUseCase;
import dev.fabian.gallardo.student.domain.port.in.UpdateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.GetSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.LoginUseCase;
import dev.fabian.gallardo.student.domain.port.in.RefreshTokenUseCase;
import dev.fabian.gallardo.student.infrastructure.config.JwtSecurityConfig;
import com.nacionservicios.emv.jwt.autoconfigure.JwtAutoConfiguration;
import com.nacionservicios.emv.jwt.token.JwtTokenService;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Cadena de seguridad JWT real + controllers: lo que protege cada ruta y cómo viaja el refresh token. */
@WebMvcTest(controllers = {AuthController.class, SampleItemController.class}, properties = {
    "app.infra.security.mode=jwt",
    "app.infra.jwt.secret=test-only-secret-test-only-secret-0123456789",
    "app.auth.secure-cookie=true"
})
@Import(JwtSecurityConfig.class)
@ImportAutoConfiguration(JwtAutoConfiguration.class)
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokens;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshUseCase;

    @MockitoBean
    private CreateSampleItemUseCase createSampleItem;

    @MockitoBean
    private GetSampleItemUseCase getSampleItem;

    @MockitoBean
    private ListSampleItemsUseCase listSampleItems;

    @MockitoBean
    private UpdateSampleItemUseCase updateSampleItem;

    @MockitoBean
    private DeleteSampleItemUseCase deleteSampleItem;

    private static final UUID SAMPLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String LOGIN_BODY = "{\"username\":\"ana\",\"password\":\"s3cret-pass\"}";

    @Test
    void login_whenCredentialsValid_thenReturnsAccessTokenAndHardenedRefreshCookie() throws Exception {
        when(loginUseCase.login("ana", "s3cret-pass"))
                .thenReturn(new AuthTokens("access-123", "refresh-456", Duration.ofDays(7)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-123"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("refresh-456"))))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refresh_token=refresh-456"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"),
                        org.hamcrest.Matchers.containsString("Path=/api/auth"),
                        org.hamcrest.Matchers.containsString("Max-Age=604800"))));
    }

    @Test
    void login_whenCredentialsInvalid_thenUnauthorizedWithoutCookie() throws Exception {
        when(loginUseCase.login(anyString(), anyString())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void me_whenNoToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_whenAccessToken_thenReturnsUserAndRoles() throws Exception {
        var token = tokens.issueAccessToken("ana", List.of("USER", "ADMIN"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("USER"));
    }

    @Test
    void me_whenRefreshTokenUsedAsBearer_thenUnauthorized() throws Exception {
        var refresh = tokens.issueRefreshToken("ana");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_whenCookieAndRequestedWithHeader_thenNewAccessTokenAndRotatedCookie() throws Exception {
        when(refreshUseCase.refresh("old-refresh"))
                .thenReturn(new AuthTokens("new-access", "new-refresh", Duration.ofDays(7)));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh"))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refresh_token=new-refresh")));
    }

    @Test
    void refresh_whenNoCookie_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_whenRequestedWithHeaderMissing_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_whenTokenRejectedByUseCase_thenUnauthorized() throws Exception {
        when(refreshUseCase.refresh(anyString())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "bad"))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_whenCalledWithoutAccessToken_thenClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void register_whenRegistrationDisabled_thenNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"s3cret-pass\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sampleItems_whenNoToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/sample-items/" + SAMPLE_ID)).andExpect(status().isUnauthorized());
    }

    @Test
    void sampleItems_whenAccessToken_thenOk() throws Exception {
        when(getSampleItem.getById(SAMPLE_ID)).thenReturn(new SampleItem(SAMPLE_ID, "ejemplo", null, 1, BigDecimal.TEN));
        var token = tokens.issueAccessToken("ana", List.of("USER"));

        mockMvc.perform(get("/api/v1/sample-items/" + SAMPLE_ID).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ejemplo"));
    }
}
