package dev.fabian.gallardo.student.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.fabian.gallardo.student.api.rest.RegistrationController;
import dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.in.RegisterUserUseCase;
import dev.fabian.gallardo.student.infrastructure.config.JwtSecurityConfig;
import com.nacionservicios.emv.jwt.autoconfigure.JwtAutoConfiguration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RegistrationController.class, properties = {
    "app.infra.security.mode=jwt",
    "app.infra.jwt.secret=test-only-secret-test-only-secret-0123456789",
    "app.auth.registration-enabled=true"
})
@Import(JwtSecurityConfig.class)
@ImportAutoConfiguration(JwtAutoConfiguration.class)
class RegistrationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUseCase;

    private static String body(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void register_whenNewUser_thenCreatedWithoutPublicToken() throws Exception {
        when(registerUseCase.register("ana", "s3cret-pass"))
                .thenReturn(new AppUser("id-1", "ana", "hash", Set.of(Role.USER)));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("ana", "s3cret-pass")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_whenUsernameTaken_thenConflict() throws Exception {
        when(registerUseCase.register(anyString(), anyString())).thenThrow(new UsernameAlreadyExistsException("ana"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("ana", "s3cret-pass")))
                .andExpect(status().isConflict());
    }

    @Test
    void register_whenPasswordTooShort_thenBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("ana", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_whenUsernameBlank_thenBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("", "s3cret-pass")))
                .andExpect(status().isBadRequest());
    }
}
