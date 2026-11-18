package dev.fabian.gallardo.student.domain.port.in;

import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;

public interface ListSampleItemsUseCase {

    Paged<SampleItem> list(int page, int size);
}
