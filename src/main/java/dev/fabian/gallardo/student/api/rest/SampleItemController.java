package dev.fabian.gallardo.student.api.rest;

import com.nacionservicios.emv.core.paging.PageResponse;
import dev.fabian.gallardo.student.api.request.SampleItemRequest;
import dev.fabian.gallardo.student.api.response.SampleItemResponse;
import dev.fabian.gallardo.student.application.dto.SampleItemDto;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.port.in.CreateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.DeleteSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.GetSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.ListSampleItemsUseCase;
import dev.fabian.gallardo.student.domain.port.in.UpdateSampleItemUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Ejemplo de CRUD hexagonal -- reemplazar por los controllers reales. Nunca poner logica de negocio aca: solo mapeo
 * request/response y llamado al use case.
 */
@RestController
@RequestMapping("/api/v1/sample-items")
public class SampleItemController {

    private final CreateSampleItemUseCase createUseCase;
    private final GetSampleItemUseCase getUseCase;
    private final ListSampleItemsUseCase listUseCase;
    private final UpdateSampleItemUseCase updateUseCase;
    private final DeleteSampleItemUseCase deleteUseCase;

    public SampleItemController(CreateSampleItemUseCase createUseCase, GetSampleItemUseCase getUseCase,
            ListSampleItemsUseCase listUseCase, UpdateSampleItemUseCase updateUseCase,
            DeleteSampleItemUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<SampleItemResponse> create(@Valid @RequestBody SampleItemRequest request) {
        var created = toResponse(createUseCase.create(request.toData()));
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<SampleItemResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = listUseCase.list(page, size);
        return PageResponse.of(
                result.content().stream().map(SampleItemController::toResponse).toList(), result.page(), result.size(),
                result.totalElements());
    }

    @GetMapping("/{id}")
    public SampleItemResponse getById(@PathVariable UUID id) {
        return toResponse(getUseCase.getById(id));
    }

    @PutMapping("/{id}")
    public SampleItemResponse update(@PathVariable UUID id, @Valid @RequestBody SampleItemRequest request) {
        return toResponse(updateUseCase.update(id, request.toData()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static SampleItemResponse toResponse(SampleItem item) {
        return SampleItemResponse.from(SampleItemDto.from(item));
    }
}
