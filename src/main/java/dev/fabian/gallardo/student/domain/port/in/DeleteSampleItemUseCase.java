package dev.fabian.gallardo.student.domain.port.in;

import java.util.UUID;

public interface DeleteSampleItemUseCase {

    void delete(UUID id);
}
