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
    SyntaxElementsOutput syntaxElements
) {

    static StructuredOutputContract<SyntaxExtractionOutput> contract() {
        return StructuredOutputContract.<SyntaxExtractionOutput>named("SyntaxExtractionOutput")
            .requiredText("syntaxElements.SUBJECT", SyntaxExtractionOutput::subject)
            .requiredText("syntaxElements.ACTION", SyntaxExtractionOutput::action)
            .requiredText("syntaxElements.OBJECT", SyntaxExtractionOutput::object)
            .optionalText("syntaxElements.CONSTRAINT", SyntaxExtractionOutput::constraint)
            .optionalText("syntaxElements.CONDITION", SyntaxExtractionOutput::condition)
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
        return syntaxElement(SyntaxElementsOutput::SUBJECT);
    }

    private String action() {
        return syntaxElement(SyntaxElementsOutput::ACTION);
    }

    private String object() {
        return syntaxElement(SyntaxElementsOutput::OBJECT);
    }

    private String constraint() {
        return syntaxElement(SyntaxElementsOutput::CONSTRAINT);
    }

    private String condition() {
        return syntaxElement(SyntaxElementsOutput::CONDITION);
    }

    private String syntaxElement(Function<SyntaxElementsOutput, String> readSyntaxElement) {
        return syntaxElements == null ? null : readSyntaxElement.apply(syntaxElements);
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
