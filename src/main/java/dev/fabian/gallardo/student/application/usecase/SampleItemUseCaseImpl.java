package dev.fabian.gallardo.student.application.usecase;

import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.exception.SampleItemNotFoundException;
import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import dev.fabian.gallardo.student.domain.port.in.CreateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.DeleteSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.GetSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.in.ListSampleItemsUseCase;
import dev.fabian.gallardo.student.domain.port.in.UpdateSampleItemUseCase;
import dev.fabian.gallardo.student.domain.port.out.SampleItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Constructor injection siempre -- nunca @Autowired en campos. */
@Service
public class SampleItemUseCaseImpl implements CreateSampleItemUseCase, GetSampleItemUseCase, ListSampleItemsUseCase,
        UpdateSampleItemUseCase, DeleteSampleItemUseCase {

    private final SampleItemRepository repository;

    public SampleItemUseCaseImpl(SampleItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public SampleItem create(SampleItemData data) {
        var item = SampleItem.create(data);
        ensureNameIsFree(item.name(), item.id());
        return repository.save(item);
    }

    @Override
    public SampleItem getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new SampleItemNotFoundException(id));
    }

    @Override
    public Paged<SampleItem> list(int page, int size) {
        return repository.findAll(page, size);
    }

    @Override
    public SampleItem update(UUID id, SampleItemData data) {
        var updated = getById(id).with(data);
        ensureNameIsFree(updated.name(), id);
        return repository.save(updated);
    }

    @Override
    public void delete(UUID id) {
        if (!repository.deleteById(id)) {
            throw new SampleItemNotFoundException(id);
        }
    }

    private void ensureNameIsFree(String name, UUID ownId) {
        repository.findByName(name).filter(other -> !other.id().equals(ownId)).ifPresent(_ -> {
            throw new SampleItemAlreadyExistsException(name);
        });
    }
}
