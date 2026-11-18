package dev.fabian.gallardo.student.application.dto;

import dev.fabian.gallardo.student.domain.model.SampleItem;
import java.math.BigDecimal;
import java.util.UUID;

/** DTO de aplicacion como record, mapeable 1:1 con SampleItem por ahora. */
public record SampleItemDto(UUID id, String name, String description, int quantity, BigDecimal price) {

    public static SampleItemDto from(SampleItem item) {
        return new SampleItemDto(item.id(), item.name(), item.description(), item.quantity(), item.price());
    }
}
