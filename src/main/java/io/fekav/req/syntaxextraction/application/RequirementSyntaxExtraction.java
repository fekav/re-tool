package io.fekav.req.syntaxextraction.application;

import io.fekav.req.shared.model.RawText;
import io.fekav.req.syntaxextraction.domain.RequirementSyntax;

public interface RequirementSyntaxExtraction {

    RequirementSyntax extractRequirementSyntax(RawText rawRequirementText);
}
