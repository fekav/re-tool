package io.fekav.req.entityextraction.model;

import java.util.Map;

import io.fekav.req.shared.model.RequirementSyntaxType;

public record RequirementSyntax(
    Map<RequirementSyntaxType, String> syntaxElements
) {
}
