package com.botica.inteligente;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;

@AnalyzeClasses(packages = "com.botica.inteligente", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_entities =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule no_field_injection_with_autowired =
            noClasses()
                    .should(new ArchCondition<JavaClass>("not have fields annotated with @Autowired") {
                        @Override
                        public void check(JavaClass item, ConditionEvents events) {
                            for (JavaField field : item.getFields()) {
                                if (field.isAnnotatedWith(Autowired.class)) {
                                    events.add(SimpleConditionEvent.violated(field,
                                            "Field " + field.getFullName() + " is annotated with @Autowired"));
                                }
                            }
                        }
                    });

    @ArchTest
    static final ArchRule controllers_should_reside_in_controller_package =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule services_should_reside_in_service_package =
            classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().resideInAPackage("..service..")
                    .orShould().resideInAPackage("..security..");

    @ArchTest
    static final ArchRule repositories_should_reside_in_repository_package =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule entities_should_reside_in_entity_package =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..entity..");
}
