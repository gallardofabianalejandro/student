package dev.fabian.gallardo.student.api.response;

import dev.fabian.gallardo.student.application.dto.SampleItemDto;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SampleItemResponse(
        UUID id, String name, @Nullable String description, int quantity, BigDecimal price) {

    public static SampleItemResponse from(SampleItemDto dto) {
        return new SampleItemResponse(dto.id(), dto.name(), dto.description(), dto.quantity(), dto.price());
    }
}
