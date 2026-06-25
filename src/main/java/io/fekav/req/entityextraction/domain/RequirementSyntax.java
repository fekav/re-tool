package io.fekav.req.entityextraction.domain;

import java.util.Map;
import java.util.Objects;

public record RequirementSyntax(
    Map<RequirementSyntaxType, String> syntaxElements
) {
    public RequirementSyntax {
        Objects.requireNonNull(syntaxElements, "syntaxElements must not be null");

        assertSyntaxElementIsPresent(syntaxElements, RequirementSyntaxType.SUBJECT);
        assertSyntaxElementIsPresent(syntaxElements, RequirementSyntaxType.ACTION);
        assertSyntaxElementIsPresent(syntaxElements, RequirementSyntaxType.OBJECT);

        syntaxElements = Map.copyOf(syntaxElements);
    }

    private static void assertSyntaxElementIsPresent(
        Map<RequirementSyntaxType, String> syntaxElements,
        RequirementSyntaxType syntaxElement
    ) {
        String extractedText = syntaxElements.get(syntaxElement);

        if (extractedText == null || extractedText.isBlank()) {
            throw new MissingRequirementSyntaxElementException(syntaxElement);
        }
    }
}
