package dev.fabian.gallardo.student.api.spa;

import com.nacionservicios.emv.jwt.autoconfigure.JwtAutoConfiguration;
import dev.fabian.gallardo.student.api.rest.AuthController;
import dev.fabian.gallardo.student.domain.port.in.LoginUseCase;
import dev.fabian.gallardo.student.domain.port.in.RefreshTokenUseCase;
import dev.fabian.gallardo.student.infrastructure.config.JwtSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con SPA: el shell y los assets son publicos, la API sigue exigiendo token. */
@WebMvcTest(controllers = {AngularForwardController.class, AuthController.class}, properties = {
    "app.infra.security.mode=jwt",
    "app.infra.jwt.secret=test-only-secret-test-only-secret-0123456789"
})
@Import(JwtSecurityConfig.class)
@ImportAutoConfiguration(JwtAutoConfiguration.class)
class SpaRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshUseCase;

    @Test
    void spaRoutes_whenRequestedWithoutToken_thenForwardToIndex() throws Exception {
        for (String route : new String[]{"/login", "/register", "/dashboard", "/dashboard/profile", "/items", "/forbidden", "/server-error"}) {
            mockMvc.perform(get(route)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void api_whenNoToken_thenStillUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/anything-else")).andExpect(status().isUnauthorized());
    }

    @Test
    void staticAssetOrUnknownPath_whenNoToken_thenNeverUnauthorized() throws Exception {
        mockMvc.perform(get("/main-ABC123.js")).andExpect(status().isNotFound());
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }
}
