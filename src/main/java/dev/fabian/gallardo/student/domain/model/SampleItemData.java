package dev.fabian.gallardo.student.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Datos editables de un SampleItem (lo que llega al crear o actualizar); la validacion la hace SampleItem. */
public record SampleItemData(String name, @Nullable String description, int quantity, BigDecimal price) {
}
