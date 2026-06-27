package io.fekav.req.syntaxextraction.application;

import java.util.Set;

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
            singleOrEmpty("syntaxElements.CONSTRAINT", action.constraints()),
            singleOrEmpty("syntaxElements.CONDITION", action.conditions())
        ));
    }

    private static String singleOrEmpty(String fieldName, Set<?> values) {
        if (values.isEmpty()) {
            return "";
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException(fieldName + " cannot represent multiple values");
        }

        Object value = values.iterator().next();
        if (value instanceof Condition condition) {
            return condition.text();
        }
        if (value instanceof Constraint constraint) {
            return constraint.text();
        }

        throw new IllegalArgumentException(fieldName + " contains unsupported value");
    }
}
