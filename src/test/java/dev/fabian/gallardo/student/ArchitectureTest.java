package dev.fabian.gallardo.student;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.enofex.taikai.Taikai;
import com.enofex.taikai.java.ImportsConfigurer;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import java.util.List;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(packages = "dev.fabian.gallardo.student", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsIsolated = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..application..",
                    "..infrastructure..",
                    "..api..",
                    "org.springframework..",
                    "jakarta.persistence..",
                    "lombok..");

    @ArchTest
    static final ArchRule applicationOnlyDependsOnDomain = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..api..");

    @ArchTest
    static final ArchRule entitiesLiveInInfrastructure = noClasses()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideOutsideOfPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule restControllersLiveInApi = classes()
            .that()
            .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should()
            .resideInAPackage("..api..");

    @ArchTest
    static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule noStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule noGenericExceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @Test
    void nullMarked_whenInspectingDomainAndApplication_thenEveryPackageIsMarked() {
        List<String> unmarked = new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("dev.fabian.gallardo.student.domain", "dev.fabian.gallardo.student.application")
                        .stream()
                        .map(JavaClass::getPackage)
                        .distinct()
                        .filter(pkg -> pkg.tryGetPackageInfo()
                                .map(info -> !info.isAnnotatedWith("org.jspecify.annotations.NullMarked"))
                                .orElse(true))
                        .map(JavaPackage::getName)
                        .sorted()
                        .toList();
        org.assertj.core.api.Assertions.assertThat(unmarked)
                .as("paquetes sin @NullMarked en package-info.java")
                .isEmpty();
    }

    @Test
    void taikai_whenCheckingConventions_thenAllPass() {
        Taikai.builder()
                .namespace("dev.fabian.gallardo.student")
                // HttpSecurity.build() declara throws Exception: no se puede evitar en la config de seguridad.
                .excludeClasses("dev.fabian.gallardo.student.infrastructure.config.JwtSecurityConfig")
                // HttpSecurity.build() declara throws Exception: no se puede evitar en la config de seguridad.
                .excludeClasses("dev.fabian.gallardo.student.infrastructure.config.OpenSecurityConfig")
                .java(java -> java.noUsageOfDeprecatedAPIs()
                        .methodsShouldNotDeclareGenericExceptions()
                        .utilityClassesShouldBeFinalAndHavePrivateConstructor()
                        .imports(ImportsConfigurer::shouldHaveNoCycles)
                        .naming(naming ->
                                naming.constantsShouldFollowConventions().interfacesShouldNotHavePrefixI()))
                .test(test -> test.junit(
                        junit -> junit.classesShouldBePackagePrivate(JavaClass.Predicates.simpleNameEndingWith("Test")
                                        .or(JavaClass.Predicates.simpleNameEndingWith("Tests")))
                                .classesShouldNotBeAnnotatedWithDisabled()
                                .methodsShouldNotBeAnnotatedWithDisabled()))
                .spring(spring -> spring.noAutowiredFields()
                        .boot(boot -> boot.applicationClassShouldResideInPackage("dev.fabian.gallardo.student"))
                        .configurations(config -> config.namesShouldMatch(".+(Config|Guard)"))
                        .controllers(controllers ->
                                controllers.namesShouldEndWithController().shouldNotDependOnOtherControllers())
                        .repositories(repositories -> repositories.shouldNotDependOnServices()))
                .build()
                .checkAll();
    }
}
