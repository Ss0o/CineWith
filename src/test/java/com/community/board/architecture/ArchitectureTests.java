package com.community.board.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.community.board")
class ArchitectureTests {

    @ArchTest
    static final ArchRule controllersMustNotDependOnRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("controllers must access repositories through services")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesMustNotDependOnControllers = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("repositories must not depend on the web layer")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule servicesMustNotDependOnControllers = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("services must not depend on the web layer")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainAndEntitiesMustNotDependOnControllers = noClasses()
            .that().resideInAnyPackage("..domain..", "..entity..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .because("domain and entity code must remain independent of the web layer")
            .allowEmptyShould(true);
}
