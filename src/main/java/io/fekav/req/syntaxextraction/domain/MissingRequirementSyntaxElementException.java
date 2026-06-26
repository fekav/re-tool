package io.fekav.req.syntaxextraction.domain;

import java.util.Locale;

public final class MissingRequirementSyntaxElementException extends RuntimeException {

    public MissingRequirementSyntaxElementException(RequirementSyntaxType syntaxElement) {
        super("requirement syntax " + syntaxElement.name().toLowerCase(Locale.ROOT) + " is required");
    }
}
