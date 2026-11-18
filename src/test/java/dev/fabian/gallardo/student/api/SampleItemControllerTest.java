package dev.fabian.gallardo.student.api;

import dev.fabian.gallardo.student.api.rest.SampleItemController;
import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.exception.SampleItemNotFoundException;
import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import dev.fabian.gallardo.student.domain.port.in.CreateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.DeleteSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.GetSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.ListSampleItemsUseCase;
import dev.fabian.gallardo.student.domain.port.in.UpdateSampleItemUseCase;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SampleItemController.class)
@WithMockUser
class SampleItemControllerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String BODY = """
            {"name":"nuevo","description":"desc","quantity":3,"price":12.5}""";

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

    private static SampleItem item(String name) {
        return new SampleItem(ID, name, "desc", 3, new BigDecimal("12.50"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    @Test
    void getById_whenExists_thenReturns200() throws Exception {
        when(getUseCase.getById(ID)).thenReturn(item("ejemplo"));

        mockMvc.perform(get("/api/v1/sample-items/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ejemplo"))
                .andExpect(jsonPath("$.price").value(12.5));
    }

    @Test
    void getById_whenNotFound_thenReturns404WithErrorCode() throws Exception {
        when(getUseCase.getById(ID)).thenThrow(new SampleItemNotFoundException(ID));

        mockMvc.perform(get("/api/v1/sample-items/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SAMPLE_ITEM_NOT_FOUND"));
    }

    @Test
    void getById_whenIdIsNotAUuid_thenReturns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/sample-items/no-es-uuid")).andExpect(status().is4xxClientError());
    }

    @Test
    void list_whenDefaults_thenReturnsPageWithMetadata() throws Exception {
        when(listUseCase.list(0, 20)).thenReturn(new Paged<>(List.of(item("a"), item("b")), 0, 20, 2));

        mockMvc.perform(get("/api/v1/sample-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void list_whenSizeAboveLimit_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/sample-items").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void list_whenNegativePage_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/sample-items").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void create_whenValidBody_thenReturns201WithLocation() throws Exception {
        when(createUseCase.create(any(SampleItemData.class))).thenReturn(item("nuevo"));

        mockMvc.perform(json(post("/api/v1/sample-items"), BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/sample-items/" + ID)))
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void create_whenBodyInvalid_thenReturns4xxAndDoesNotCallUseCase() throws Exception {
        mockMvc.perform(json(post("/api/v1/sample-items"), "{\"name\":\"\",\"quantity\":-1}"))
                .andExpect(status().is4xxClientError());

        org.mockito.Mockito.verifyNoInteractions(createUseCase);
    }

    @Test
    void create_whenNameAlreadyExists_thenReturns409() throws Exception {
        when(createUseCase.create(any(SampleItemData.class))).thenThrow(new SampleItemAlreadyExistsException("nuevo"));

        mockMvc.perform(json(post("/api/v1/sample-items"), BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SAMPLE_ITEM_ALREADY_EXISTS"));
    }

    @Test
    void update_whenExists_thenReturns200() throws Exception {
        when(updateUseCase.update(eq(ID), any(SampleItemData.class))).thenReturn(item("nuevo"));

        mockMvc.perform(json(put("/api/v1/sample-items/" + ID), BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nuevo"));
    }

    @Test
    void update_whenNotFound_thenReturns404() throws Exception {
        when(updateUseCase.update(eq(ID), any(SampleItemData.class))).thenThrow(new SampleItemNotFoundException(ID));

        mockMvc.perform(json(put("/api/v1/sample-items/" + ID), BODY)).andExpect(status().isNotFound());
    }

    @Test
    void delete_whenExists_thenReturns204() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/sample-items/" + ID).with(csrf()))
                .andExpect(status().isNoContent());

        verify(deleteUseCase).delete(ID);
    }

    @Test
    void delete_whenNotFound_thenReturns404() throws Exception {
        doThrow(new SampleItemNotFoundException(ID)).when(deleteUseCase).delete(ID);

        mockMvc.perform(
                        delete("/api/v1/sample-items/" + ID).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
