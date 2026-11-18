package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que el jar realmente sirve el front compilado por Maven. Se omite si se construyó con
 * -Dskip.frontend (no hay index.html en el classpath).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SpaServingIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    SpaServingIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void indexAndAssets_whenFrontendBuilt_thenServedWithoutToken() throws Exception {
        assumeTrue(new ClassPathResource("static/index.html").exists(), "frontend no construido");

        String html = mockMvc.perform(get("/index.html")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        assertThat(html).contains("<app-root>");

        var script = Pattern.compile("src=\"(main-[^\"]+\\.js)\"").matcher(html);
        assertThat(script.find()).as("referencia al bundle principal").isTrue();
        mockMvc.perform(get("/" + script.group(1))).andExpect(status().isOk());

        mockMvc.perform(get("/dashboard")).andExpect(forwardedUrl("/index.html"));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}
