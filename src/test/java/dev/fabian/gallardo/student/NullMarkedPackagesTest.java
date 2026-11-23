package dev.fabian.gallardo.student;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * NullAway corre con OnlyNullMarked: un paquete sin @NullMarked en su package-info no se revisa y el olvido pasa en
 * silencio. Este test obliga a que todo paquete con clases de produccion lo declare.
 *
 * <p>El paquete raiz solo tiene Application y se excluye a proposito: @NullMarked aplica por nombre de paquete, asi que
 * marcarlo tambien someteria a NullAway las clases de test que viven en el mismo paquete.
 */
class NullMarkedPackagesTest {

    private static final String BASE_PACKAGE = "dev.fabian.gallardo.student";

    @Test
    void packages_whenTheyContainProductionClasses_thenAreNullMarked() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        List<String> withoutNullMarked = classes.stream()
                .filter(javaClass -> !javaClass.getSimpleName().equals("package-info"))
                .map(JavaClass::getPackage)
                .distinct()
                .filter(javaPackage -> !javaPackage.getName().equals(BASE_PACKAGE))
                .filter(javaPackage -> !isNullMarked(javaPackage))
                .map(JavaPackage::getName)
                .sorted()
                .toList();

        assertThat(withoutNullMarked)
                .as("Paquetes con clases pero sin @NullMarked en su package-info (NullAway los ignora)")
                .isEmpty();
    }

    private static boolean isNullMarked(JavaPackage javaPackage) {
        return javaPackage
                .tryGetPackageInfo()
                .map(info -> info.isAnnotatedWith(NullMarked.class))
                .orElse(false);
    }
}
