package dev.fabian.gallardo.student.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.fabian.gallardo.student.domain.model.Paged;
import dev.fabian.gallardo.student.domain.model.SampleItem;
import dev.fabian.gallardo.student.domain.model.SampleItemData;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit puro, sin Spring: las invariantes viven en el constructor del record. */
class SampleItemTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void constructor_whenValid_thenNormalizesTextAndPrice() {
        var item = new SampleItem(ID, "  Teclado  ", "   ", 3, new BigDecimal("9.999"));

        assertThat(item.name()).isEqualTo("Teclado");
        assertThat(item.description()).isNull();
        assertThat(item.price()).isEqualByComparingTo("10.00");
        assertThat(item.price().scale()).isEqualTo(2);
    }

    @Test
    void constructor_whenNameBlank_thenThrows() {
        assertThatThrownBy(() -> new SampleItem(ID, " ", null, 1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void constructor_whenNameTooLong_thenThrows() {
        assertThatThrownBy(() -> new SampleItem(ID, "x".repeat(101), null, 1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_whenDescriptionTooLong_thenThrows() {
        assertThatThrownBy(() -> new SampleItem(ID, "ok", "x".repeat(501), 1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_whenNegativeQuantityOrPrice_thenThrows() {
        assertThatThrownBy(() -> new SampleItem(ID, "ok", null, -1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SampleItem(ID, "ok", null, 1, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_whenIdOrPriceNull_thenThrows() {
        assertThatThrownBy(() -> new SampleItem(null, "ok", null, 1, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SampleItem(ID, "ok", null, 1, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void create_whenData_thenGeneratesNewId() {
        var data = new SampleItemData("Mouse", "inalambrico", 2, BigDecimal.TEN);

        var first = SampleItem.create(data);
        var second = SampleItem.create(data);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.name()).isEqualTo("Mouse");
    }

    @Test
    void with_whenNewData_thenKeepsIdAndReplacesTheRest() {
        var original = new SampleItem(ID, "Mouse", null, 1, BigDecimal.ONE);

        var changed = original.with(new SampleItemData("Mouse gamer", "rgb", 5, new BigDecimal("25")));

        assertThat(changed.id()).isEqualTo(ID);
        assertThat(changed.name()).isEqualTo("Mouse gamer");
        assertThat(changed.quantity()).isEqualTo(5);
    }

    @Test
    void paged_whenMapped_thenKeepsMetadataAndIsImmutable() {
        var paged = new Paged<>(List.of(1, 2), 1, 2, 7).map(n -> "n" + n);

        assertThat(paged.content()).containsExactly("n1", "n2");
        assertThat(paged.page()).isEqualTo(1);
        assertThat(paged.totalElements()).isEqualTo(7);
        assertThatThrownBy(() -> paged.content().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
