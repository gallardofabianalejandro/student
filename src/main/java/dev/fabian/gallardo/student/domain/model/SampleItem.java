package dev.fabian.gallardo.student.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Ejemplo de modelo de dominio -- reemplazar por las entidades reales del negocio. Record inmutable, sin Lombok y
 * sin dependencias externas (regla dura de domain/): las invariantes se validan en el constructor compacto.
 */
public record SampleItem(UUID id, String name, String description, int quantity, BigDecimal price) {

    private static final int NAME_MAX = 100;
    private static final int DESCRIPTION_MAX = 500;

    public SampleItem {
        Objects.requireNonNull(id, "id es obligatorio");
        name = requiredText(name, "name", NAME_MAX);
        description = optionalText(description, "description", DESCRIPTION_MAX);
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity no puede ser negativo");
        }
        Objects.requireNonNull(price, "price es obligatorio");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price no puede ser negativo");
        }
        price = price.setScale(2, RoundingMode.HALF_UP);
    }

    public static SampleItem create(SampleItemData data) {
        return new SampleItem(UUID.randomUUID(), data.name(), data.description(), data.quantity(), data.price());
    }

    /** Copia con los datos nuevos y el mismo id (los records son inmutables). */
    public SampleItem with(SampleItemData data) {
        return new SampleItem(id, data.name(), data.description(), data.quantity(), data.price());
    }

    private static String requiredText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        var text = value.strip();
        if (text.length() > max) {
            throw new IllegalArgumentException(field + " no puede superar " + max + " caracteres");
        }
        return text;
    }

    private static String optionalText(String value, String field, int max) {
        return value == null || value.isBlank() ? null : requiredText(value, field, max);
    }
}
