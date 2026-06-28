package io.fekav.req.syntaxextraction.application;

import java.util.Set;

public record SyntaxElementsResponse(
    String SUBJECT,
    String ACTION,
    String OBJECT,
    Set<String> CONSTRAINT,
    Set<String> CONDITION
) {
}
