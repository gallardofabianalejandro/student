package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.SampleItem;
import java.util.UUID;

public interface GetSampleItemUseCase {

    SampleItem getById(UUID id);
}
