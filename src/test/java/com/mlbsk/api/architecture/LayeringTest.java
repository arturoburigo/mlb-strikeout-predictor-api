package com.mlbsk.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.mlbsk.api", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

    @ArchTest
    static final ArchRule domainMustNotImportFrameworks = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "io.micronaut..",
            "io.r2dbc..",
            "reactor..",
            "com.fasterxml.jackson.."
        )
        .because("Domain classes should be pure and framework-agnostic");

    @ArchTest
    static final ArchRule controllerMustNotImportRepository = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..repository..")
        .because("Controllers should only access services, not repositories directly");

    @ArchTest
    static final ArchRule serviceMustNotImportConcreteRepository = noClasses()
        .that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..repository..")
        .andShould().dependOnClassesThat().haveSimpleNameStartingWith("R2dbc")
        .because("Services should not depend on repository implementations");
}
