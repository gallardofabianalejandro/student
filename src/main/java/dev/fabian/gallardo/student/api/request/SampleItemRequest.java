package dev.fabian.gallardo.student.api.request;

import dev.fabian.gallardo.student.domain.model.SampleItemData;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Cuerpo de POST y PUT. Las anotaciones validan el formato; las reglas del negocio viven en el dominio. */
public record SampleItemRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @PositiveOrZero Integer quantity,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price) {

    public SampleItemData toData() {
        return new SampleItemData(name, description, quantity, price);
    }
}
