package dev.fabian.gallardo.student.infrastructure.persistence;

import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.port.out.SampleItemRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del puerto SampleItemRepository (PostgreSQL + Flyway: db/migration/V2__create_sample_item.sql). */
@Repository
public class JpaSampleItemRepositoryAdapter implements SampleItemRepository {

    private final SampleItemJpaRepository jpa;

    public JpaSampleItemRepositoryAdapter(SampleItemJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public SampleItem save(SampleItem item) {
        var entity = jpa.findById(item.id()).map(existing -> {
            existing.apply(item);
            return existing;
        }).orElseGet(() -> SampleItemEntity.from(item));
        try {
            return jpa.saveAndFlush(entity).toDomain();
        } catch (DataIntegrityViolationException _) {
            throw new SampleItemAlreadyExistsException(item.name());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleItem> findById(UUID id) {
        return jpa.findById(id).map(SampleItemEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleItem> findByName(String name) {
        return jpa.findByName(name).map(SampleItemEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Paged<SampleItem> findAll(int page, int size) {
        var result = jpa.findAll(PageRequest.of(page, size, Sort.by("name")));
        return new Paged<>(result.getContent().stream().map(SampleItemEntity::toDomain).toList(), page, size,
                result.getTotalElements());
    }

    @Override
    @Transactional
    public boolean deleteById(UUID id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }
}
