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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(SampleItemController.class)
@WithMockUser
class SampleItemControllerTest {

    private static final String BASE = "/api/v1/sample-items";
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String BODY = """
            {"name":"nuevo","description":"desc","quantity":3,"price":12.5}""";

    @Autowired
    private MockMvcTester mvc;

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

    private static MockMvcRequestBuilder withCsrf(MockMvcRequestBuilder builder) {
        return builder.with(csrf());
    }

    private static MockMvcRequestBuilder json(MockMvcRequestBuilder builder, String body) {
        return withCsrf(builder).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void getById_whenExists_thenReturns200() {
        when(getUseCase.getById(ID)).thenReturn(item("ejemplo"));

        var response = assertThat(mvc.get().uri(BASE + "/" + ID)).hasStatusOk().bodyJson();

        response.extractingPath("$.name").isEqualTo("ejemplo");
        response.extractingPath("$.price").isEqualTo(12.5);
    }

    @Test
    void getById_whenNotFound_thenReturns404WithErrorCode() {
        when(getUseCase.getById(ID)).thenThrow(new SampleItemNotFoundException(ID));

        assertThat(mvc.get().uri(BASE + "/" + ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.errorCode")
                .isEqualTo("SAMPLE_ITEM_NOT_FOUND");
    }

    @Test
    void getById_whenIdIsNotAUuid_thenReturns4xx() {
        assertThat(mvc.get().uri(BASE + "/no-es-uuid")).hasStatus4xxClientError();
    }

    @Test
    void list_whenDefaults_thenReturnsPageWithMetadata() {
        when(listUseCase.list(0, 20)).thenReturn(new Paged<>(List.of(item("a"), item("b")), 0, 20, 2));

        var response = assertThat(mvc.get().uri(BASE)).hasStatusOk().bodyJson();

        response.extractingPath("$.content").asArray().hasSize(2);
        response.extractingPath("$.totalElements").isEqualTo(2);
        response.extractingPath("$.totalPages").isEqualTo(1);
        response.extractingPath("$.first").isEqualTo(true);
        response.extractingPath("$.last").isEqualTo(true);
    }

    @Test
    void list_whenSizeAboveLimit_thenReturns400() {
        assertThat(mvc.get().uri(BASE).param("size", "101")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void list_whenNegativePage_thenReturns400() {
        assertThat(mvc.get().uri(BASE).param("page", "-1")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_whenValidBody_thenReturns201WithLocation() {
        when(createUseCase.create(any(SampleItemData.class))).thenReturn(item("nuevo"));

        var response = assertThat(json(mvc.post().uri(BASE), BODY)).hasStatus(HttpStatus.CREATED);

        response.headers().hasHeaderSatisfying("Location", values -> assertThat(values.get(0)).endsWith(BASE + "/" + ID));
        response.bodyJson().extractingPath("$.id").isEqualTo(ID.toString());
    }

    @Test
    void create_whenBodyInvalid_thenReturns4xxAndDoesNotCallUseCase() {
        assertThat(json(mvc.post().uri(BASE), "{\"name\":\"\",\"quantity\":-1}")).hasStatus4xxClientError();

        verifyNoInteractions(createUseCase);
    }

    @Test
    void create_whenNameAlreadyExists_thenReturns409() {
        when(createUseCase.create(any(SampleItemData.class))).thenThrow(new SampleItemAlreadyExistsException("nuevo"));

        assertThat(json(mvc.post().uri(BASE), BODY))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.errorCode")
                .isEqualTo("SAMPLE_ITEM_ALREADY_EXISTS");
    }

    @Test
    void update_whenExists_thenReturns200() {
        when(updateUseCase.update(eq(ID), any(SampleItemData.class))).thenReturn(item("nuevo"));

        assertThat(json(mvc.put().uri(BASE + "/" + ID), BODY))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("nuevo");
    }

    @Test
    void update_whenNotFound_thenReturns404() {
        when(updateUseCase.update(eq(ID), any(SampleItemData.class))).thenThrow(new SampleItemNotFoundException(ID));

        assertThat(json(mvc.put().uri(BASE + "/" + ID), BODY)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void delete_whenExists_thenReturns204() {
        assertThat(withCsrf(mvc.delete().uri(BASE + "/" + ID))).hasStatus(HttpStatus.NO_CONTENT);

        verify(deleteUseCase).delete(ID);
    }

    @Test
    void delete_whenNotFound_thenReturns404() {
        doThrow(new SampleItemNotFoundException(ID)).when(deleteUseCase).delete(ID);

        assertThat(withCsrf(mvc.delete().uri(BASE + "/" + ID))).hasStatus(HttpStatus.NOT_FOUND);
    }
}
