package io.fekav.req.syntaxextraction.application;

import java.util.stream.Collectors;

import io.fekav.req.syntaxextraction.domain.Action;
import io.fekav.req.syntaxextraction.domain.Condition;
import io.fekav.req.syntaxextraction.domain.Constraint;

public record ExtractSyntaxResponse(
    SyntaxElementsResponse syntaxElements
) {

    public static ExtractSyntaxResponse from(Action action) {
        return new ExtractSyntaxResponse(new SyntaxElementsResponse(
            action.subject().text(),
            action.actionText(),
            action.targetObject().text(),
            action.constraints().stream()
                .map(Constraint::text)
                .collect(Collectors.toUnmodifiableSet()),
            action.conditions().stream()
                .map(Condition::text)
                .collect(Collectors.toUnmodifiableSet())
        ));
    }
}
