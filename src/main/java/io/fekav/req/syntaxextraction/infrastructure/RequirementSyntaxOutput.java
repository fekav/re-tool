package io.fekav.req.syntaxextraction.infrastructure;

import java.util.EnumMap;
import java.util.function.Function;

import io.fekav.platform.structuredoutput.StructuredOutputContract;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;
import io.fekav.req.syntaxextraction.domain.RequirementSyntaxType;

public record RequirementSyntaxOutput(
    RequirementSyntaxElementsOutput syntaxElements
) {

    static StructuredOutputContract<RequirementSyntaxOutput> contract() {
        return StructuredOutputContract.<RequirementSyntaxOutput>named("RequirementSyntaxOutput")
            .requiredText("syntaxElements.SUBJECT", RequirementSyntaxOutput::subject)
            .requiredText("syntaxElements.ACTION", RequirementSyntaxOutput::action)
            .requiredText("syntaxElements.OBJECT", RequirementSyntaxOutput::object)
            .optionalText("syntaxElements.CONSTRAINT", RequirementSyntaxOutput::constraint)
            .optionalText("syntaxElements.CONDITION", RequirementSyntaxOutput::condition)
            .build();
    }

    RequirementSyntax toRequirementSyntax() {
        EnumMap<RequirementSyntaxType, String> extractedSyntax = new EnumMap<>(RequirementSyntaxType.class);

        extractedSyntax.put(RequirementSyntaxType.SUBJECT, subject());
        extractedSyntax.put(RequirementSyntaxType.ACTION, action());
        extractedSyntax.put(RequirementSyntaxType.OBJECT, object());
        putOptionalSyntaxElement(extractedSyntax, RequirementSyntaxType.CONSTRAINT, constraint());
        putOptionalSyntaxElement(extractedSyntax, RequirementSyntaxType.CONDITION, condition());

        return new RequirementSyntax(extractedSyntax);
    }

    private String subject() {
        return syntaxElement(RequirementSyntaxElementsOutput::SUBJECT);
    }

    private String action() {
        return syntaxElement(RequirementSyntaxElementsOutput::ACTION);
    }

    private String object() {
        return syntaxElement(RequirementSyntaxElementsOutput::OBJECT);
    }

    private String constraint() {
        return syntaxElement(RequirementSyntaxElementsOutput::CONSTRAINT);
    }

    private String condition() {
        return syntaxElement(RequirementSyntaxElementsOutput::CONDITION);
    }

    private String syntaxElement(Function<RequirementSyntaxElementsOutput, String> readSyntaxElement) {
        return syntaxElements == null ? null : readSyntaxElement.apply(syntaxElements);
    }

    private static void putOptionalSyntaxElement(
        EnumMap<RequirementSyntaxType, String> extractedSyntax,
        RequirementSyntaxType syntaxType,
        String extractedText
    ) {
        if (extractedText != null && !extractedText.isBlank()) {
            extractedSyntax.put(syntaxType, extractedText);
        }
    }
}
