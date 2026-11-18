package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;

public interface CreateSampleItemUseCase {

    SampleItem create(SampleItemData data);
}
