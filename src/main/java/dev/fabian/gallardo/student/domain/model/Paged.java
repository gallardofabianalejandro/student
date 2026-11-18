package dev.fabian.gallardo.student.domain.model;

import java.util.List;
import java.util.function.Function;

/** Pagina de resultados del dominio (sin Spring Data). {@code page} arranca en 0. */
public record Paged<T>(List<T> content, int page, int size, long totalElements) {

    public Paged {
        content = List.copyOf(content);
    }

    public <R> Paged<R> map(Function<? super T, ? extends R> mapper) {
        return new Paged<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }
}
