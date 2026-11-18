package dev.fabian.gallardo.student.api;

import dev.fabian.gallardo.student.api.rest.SampleItemController;
import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import dev.fabian.gallardo.student.domain.port.in.CreateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.DeleteSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.GetSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.ListSampleItemsUseCase;
import dev.fabian.gallardo.student.domain.port.in.UpdateSampleItemUseCase;
import dev.fabian.gallardo.student.infrastructure.config.OpenSecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Con app.auth.enabled=false la API responde sin credenciales ni CSRF. */
@WebMvcTest(controllers = SampleItemController.class, properties = "app.auth.enabled=false")
@Import(OpenSecurityConfig.class)
class AuthSwitchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSampleItemUseCase createUseCase;

    @MockitoBean
    private GetSampleItemUseCase getUseCase;

    @MockitoBean
    private ListSampleItemsUseCase listUseCase;

    @MockitoBean
    private UpdateSampleItemUseCase updateUseCase;

    @MockitoBean
    private DeleteSampleItemUseCase deleteUseCase;

    @Test
    void list_whenLoginIsOff_thenAnswersWithoutCredentials() throws Exception {
        when(listUseCase.list(0, 20)).thenReturn(new Paged<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/sample-items")).andExpect(status().isOk());
    }

    @Test
    void create_whenLoginIsOff_thenNeedsNeitherTokenNorCsrf() throws Exception {
        var id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(createUseCase.create(any(SampleItemData.class))).thenReturn(new SampleItem(id, "abierto", null, 1, BigDecimal.ONE));

        mockMvc.perform(post("/api/v1/sample-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"abierto\",\"quantity\":1,\"price\":1}"))
                .andExpect(status().isCreated());
    }
}
