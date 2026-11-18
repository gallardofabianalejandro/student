package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import java.util.UUID;

public interface UpdateSampleItemUseCase {

    SampleItem update(UUID id, SampleItemData data);
}
