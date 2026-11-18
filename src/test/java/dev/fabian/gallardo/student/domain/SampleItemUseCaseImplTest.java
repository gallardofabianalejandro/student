package dev.fabian.gallardo.student.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.fabian.gallardo.student.application.usecase.SampleItemUseCaseImpl;
import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.exception.SampleItemNotFoundException;
import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import dev.fabian.gallardo.student.domain.port.out.SampleItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit puro, sin Spring context -- 90% cobertura minima en domain/application. */
class SampleItemUseCaseImplTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final SampleItemData DATA = new SampleItemData("ejemplo", "desc", 2, BigDecimal.TEN);

    private final SampleItemRepository repository = mock(SampleItemRepository.class);
    private final SampleItemUseCaseImpl useCase = new SampleItemUseCaseImpl(repository);

    @Test
    void create_whenNameIsFree_thenSavesAndReturnsItem() {
        when(repository.findByName("ejemplo")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = useCase.create(DATA);

        assertThat(created.name()).isEqualTo("ejemplo");
        assertThat(created.id()).isNotNull();
        verify(repository).save(any(SampleItem.class));
    }

    @Test
    void create_whenNameAlreadyUsed_thenThrowsAndDoesNotSave() {
        when(repository.findByName("ejemplo")).thenReturn(Optional.of(new SampleItem(ID, "ejemplo", null, 1, BigDecimal.ONE)));

        assertThatThrownBy(() -> useCase.create(DATA)).isInstanceOf(SampleItemAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getById_whenNotFound_thenThrowsDomainException() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getById(ID)).isInstanceOf(SampleItemNotFoundException.class);
    }

    @Test
    void getById_whenExists_thenReturnsIt() {
        var item = new SampleItem(ID, "ejemplo", null, 1, BigDecimal.ONE);
        when(repository.findById(ID)).thenReturn(Optional.of(item));

        assertThat(useCase.getById(ID)).isEqualTo(item);
    }

    @Test
    void list_whenCalled_thenDelegatesPaging() {
        var page = new Paged<>(List.of(new SampleItem(ID, "a", null, 1, BigDecimal.ONE)), 1, 5, 6);
        when(repository.findAll(1, 5)).thenReturn(page);

        assertThat(useCase.list(1, 5)).isEqualTo(page);
    }

    @Test
    void update_whenExistsAndNameFree_thenSavesChanges() {
        when(repository.findById(ID)).thenReturn(Optional.of(new SampleItem(ID, "viejo", null, 1, BigDecimal.ONE)));
        when(repository.findByName("ejemplo")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = useCase.update(ID, DATA);

        assertThat(updated.id()).isEqualTo(ID);
        assertThat(updated.name()).isEqualTo("ejemplo");
        assertThat(updated.quantity()).isEqualTo(2);
    }

    @Test
    void update_whenKeepingItsOwnName_thenIsNotAConflict() {
        var current = new SampleItem(ID, "ejemplo", null, 1, BigDecimal.ONE);
        when(repository.findById(ID)).thenReturn(Optional.of(current));
        when(repository.findByName("ejemplo")).thenReturn(Optional.of(current));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(useCase.update(ID, DATA).quantity()).isEqualTo(2);
    }

    @Test
    void update_whenNameBelongsToAnotherItem_thenThrowsConflict() {
        when(repository.findById(ID)).thenReturn(Optional.of(new SampleItem(ID, "viejo", null, 1, BigDecimal.ONE)));
        when(repository.findByName("ejemplo")).thenReturn(Optional.of(new SampleItem(OTHER_ID, "ejemplo", null, 1, BigDecimal.ONE)));

        assertThatThrownBy(() -> useCase.update(ID, DATA)).isInstanceOf(SampleItemAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void update_whenNotFound_thenThrows() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.update(ID, DATA)).isInstanceOf(SampleItemNotFoundException.class);
    }

    @Test
    void delete_whenExists_thenDeletes() {
        when(repository.deleteById(ID)).thenReturn(true);

        useCase.delete(ID);

        verify(repository).deleteById(ID);
    }

    @Test
    void delete_whenNotFound_thenThrows() {
        when(repository.deleteById(ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.delete(ID)).isInstanceOf(SampleItemNotFoundException.class);
    }
}
