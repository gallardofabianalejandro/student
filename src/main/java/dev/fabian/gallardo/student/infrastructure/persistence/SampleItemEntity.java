package dev.fabian.gallardo.student.infrastructure.persistence;

import dev.fabian.gallardo.student.domain.model.SampleItem;
import com.nacionservicios.emv.persistence.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA. Es una clase y no un record porque JPA necesita un constructor sin argumentos y campos mutables; el
 * modelo de dominio (SampleItem) si es un record y esta clase solo existe en infraestructura.
 */
@Entity
@Table(name = "sample_item")
public class SampleItemEntity extends BaseAuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected SampleItemEntity() {
    }

    static SampleItemEntity from(SampleItem item) {
        var entity = new SampleItemEntity();
        entity.id = item.id();
        entity.apply(item);
        return entity;
    }

    void apply(SampleItem item) {
        this.name = item.name();
        this.description = item.description();
        this.quantity = item.quantity();
        this.price = item.price();
    }

    SampleItem toDomain() {
        return new SampleItem(id, name, description, quantity, price);
    }
}
