package io.fekav.req.syntaxextraction.infrastructure;

import java.util.Set;
import java.util.function.Function;

import io.fekav.platform.structuredoutput.StructuredOutputContract;
import io.fekav.req.shared.model.ElementId;
import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;
import io.fekav.req.syntaxextraction.domain.Subject;
import io.fekav.req.syntaxextraction.domain.TargetObject;

public record SyntaxExtractionOutput(
    RequirementElementsOutput requirementElements
) {

    static StructuredOutputContract<SyntaxExtractionOutput> contract() {
        return StructuredOutputContract.<SyntaxExtractionOutput>named("SyntaxExtractionOutput")
            .requiredText("requirementElements.SUBJECT", SyntaxExtractionOutput::subject)
            .requiredText("requirementElements.ACTION", SyntaxExtractionOutput::action)
            .requiredText("requirementElements.OBJECT", SyntaxExtractionOutput::object)
            .optionalText("requirementElements.CONSTRAINT", SyntaxExtractionOutput::constraint)
            .optionalText("requirementElements.CONDITION", SyntaxExtractionOutput::condition)
            .build();
    }

    Action toAction() {
        return new Action(
            ElementId.create(),
            action(),
            new Subject(ElementId.create(), subject()),
            new TargetObject(ElementId.create(), object()),
            conditionSet(),
            constraintSet()
        );
    }

    private String subject() {
        return requirementElement(RequirementElementsOutput::SUBJECT);
    }

    private String action() {
        return requirementElement(RequirementElementsOutput::ACTION);
    }

    private String object() {
        return requirementElement(RequirementElementsOutput::OBJECT);
    }

    private String constraint() {
        return requirementElement(RequirementElementsOutput::CONSTRAINT);
    }

    private String condition() {
        return requirementElement(RequirementElementsOutput::CONDITION);
    }

    private String requirementElement(
        Function<RequirementElementsOutput, String> readRequirementElement
    ) {
        return requirementElements == null
            ? null
            : readRequirementElement.apply(requirementElements);
    }

    private Set<Constraint> constraintSet() {
        return constraint() == null || constraint().isBlank()
            ? Set.of()
            : Set.of(new Constraint(constraint()));
    }

    private Set<Condition> conditionSet() {
        return condition() == null || condition().isBlank()
            ? Set.of()
            : Set.of(new Condition(condition()));
    }
}
