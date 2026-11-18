package dev.fabian.gallardo.student.domain.port.out;

import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida -- la implementacion real va en infrastructure/persistence/. */
public interface SampleItemRepository {

    SampleItem save(SampleItem item);

    Optional<SampleItem> findById(UUID id);

    Optional<SampleItem> findByName(String name);

    /** Pagina ordenada por nombre; {@code page} arranca en 0. */
    Paged<SampleItem> findAll(int page, int size);

    /** @return true si existia y se borro. */
    boolean deleteById(UUID id);
}
