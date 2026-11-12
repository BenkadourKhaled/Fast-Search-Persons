package com.fastsearch.persons;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Les packages autonomes ne doivent dépendre ni de Spring ni de JPA, et ne
 * doivent pas remonter vers les couches d'API, de configuration ou d'audit.
 *
 * <p>Les paquets sont qualifiés par le préfixe de l'application : un motif
 * comme {@code ..api..} attraperait aussi {@code org.junit.jupiter.api}.</p>
 */
@AnalyzeClasses(packages = "com.fastsearch.persons",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] COEUR = {
            "com.fastsearch.persons.analysis..",
            "com.fastsearch.persons.names..",
            "com.fastsearch.persons.dates..",
            "com.fastsearch.persons.search.."
    };

    @ArchTest
    static final ArchRule coeur_sans_spring_ni_jpa = noClasses()
            .that().resideInAnyPackage(COEUR)
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule coeur_sans_couches_externes = noClasses()
            .that().resideInAnyPackage(COEUR)
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.fastsearch.persons.sync..",
                    "com.fastsearch.persons.eval..",
                    "com.fastsearch.persons.api..",
                    "com.fastsearch.persons.config..",
                    "com.fastsearch.persons.security..",
                    "com.fastsearch.persons.audit..");

    @ArchTest
    static final ArchRule noms_et_dates_sans_moteur = noClasses()
            .that().resideInAnyPackage("com.fastsearch.persons.names..",
                    "com.fastsearch.persons.dates..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.fastsearch.persons.search..",
                    "com.fastsearch.persons.sync..",
                    "com.fastsearch.persons.eval..",
                    "com.fastsearch.persons.api..");

    @ArchTest
    static final ArchRule api_sans_jpa = noClasses()
            .that().resideInAPackage("com.fastsearch.persons.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..");
}
