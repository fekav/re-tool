package io.fekav.req.entityextraction.domain;

import java.util.Map;

public record RequirementSyntax(
    Map<RequirementSyntaxType, String> syntaxElements
) {
}
